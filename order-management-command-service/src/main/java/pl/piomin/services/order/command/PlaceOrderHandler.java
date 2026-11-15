package pl.piomin.services.order.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.springframework.stereotype.Component;
import pl.piomin.services.order.domain.event.OrderLine;
import pl.piomin.services.order.domain.event.OrderPlaced;
import pl.piomin.services.reference.ReferenceClient.ItemRef;
import pl.piomin.services.reference.ReferenceData;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks the person, address and items against the reference service and places the order. Item name and price are
 * copied into the event, so later changes to the item do not change orders already placed.
 */
@Component
public class PlaceOrderHandler {

    private final ReferenceData referenceData;
    private final Clock clock;

    public PlaceOrderHandler(ReferenceData referenceData, Clock clock) {
        this.referenceData = referenceData;
        this.clock = clock;
    }

    /** @return the new order's version, 0 */
    @CommandHandler
    public long handle(PlaceOrderCommand command, EventAppender eventAppender) {
        referenceData.requirePerson(command.personId());
        referenceData.requireAddressOf(command.addressId(), command.personId());

        // the same item listed twice becomes one line with the summed quantity; an unknown item is reported on the
        // first request line that names it
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Map<Long, Integer> firstIndex = new HashMap<>();
        for (int i = 0; i < command.lines().size(); i++) {
            PlaceOrderCommand.Line line = command.lines().get(i);
            quantities.merge(line.itemId(), line.quantity(), Integer::sum);
            firstIndex.putIfAbsent(line.itemId(), i);
        }

        List<OrderLine> lines = quantities.entrySet().stream()
                .map(entry -> {
                    ItemRef item = referenceData.requireItem(entry.getKey(),
                            "lines[" + firstIndex.get(entry.getKey()) + "].itemId");
                    return new OrderLine(item.id(), item.sku(), item.name(), entry.getValue(), item.price());
                })
                .toList();
        BigDecimal total = lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        eventAppender.append(new OrderPlaced(command.orderId(), command.personId(), command.addressId(), lines, total,
                clock.instant()));
        return 0L;
    }
}
