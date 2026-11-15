package pl.piomin.services.order.domain;

import pl.piomin.services.order.domain.event.OrderCancelled;
import pl.piomin.services.order.domain.event.OrderDelivered;
import pl.piomin.services.order.domain.event.OrderLine;
import pl.piomin.services.order.domain.event.OrderPaid;
import pl.piomin.services.order.domain.event.OrderPlaced;
import pl.piomin.services.order.domain.event.OrderShipped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Builds an order in a given status the way Axon does: by applying its events. */
public final class OrderStates {

    public static final BigDecimal TOTAL = new BigDecimal("39.98");

    private OrderStates() {
    }

    public static Order in(OrderStatus status) {
        UUID id = UUID.randomUUID();
        Order order = new Order();
        order.on(new OrderPlaced(id, 1L, 2L, List.of(new OrderLine(10L, "SKU-10", "Mug", 2, new BigDecimal("19.99"))),
                TOTAL, Instant.now()));
        switch (status) {
            case PLACED -> {
            }
            case PAID -> order.on(new OrderPaid(id, UUID.randomUUID()));
            case SHIPPED -> {
                order.on(new OrderPaid(id, UUID.randomUUID()));
                order.on(new OrderShipped(id, UUID.randomUUID()));
            }
            case DELIVERED -> {
                order.on(new OrderPaid(id, UUID.randomUUID()));
                order.on(new OrderShipped(id, UUID.randomUUID()));
                order.on(new OrderDelivered(id, UUID.randomUUID()));
            }
            case CANCELLED -> order.on(new OrderCancelled(id, "test"));
        }
        return order;
    }
}
