package pl.piomin.services.delivery.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.springframework.stereotype.Component;
import pl.piomin.services.delivery.domain.event.DeliveryScheduled;
import pl.piomin.services.order.domain.Order;
import pl.piomin.services.order.domain.Orders;

@Component
public class ScheduleDeliveryHandler {

    /** @return the new aggregate's version, 0 */
    @CommandHandler
    public long handle(ScheduleDeliveryCommand command, ProcessingContext context, EventAppender eventAppender) {
        Order order = Orders.load(command.orderId(), context);
        order.acceptsDelivery();
        eventAppender.append(new DeliveryScheduled(command.deliveryId(), command.orderId(), order.addressId(),
                command.carrier(), command.scheduledFor()));
        return 0L;
    }
}
