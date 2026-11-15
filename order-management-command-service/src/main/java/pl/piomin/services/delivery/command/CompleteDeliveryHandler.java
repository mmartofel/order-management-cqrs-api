package pl.piomin.services.delivery.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.delivery.domain.Delivery;
import pl.piomin.services.delivery.domain.event.DeliveryCompleted;
import pl.piomin.services.order.domain.Orders;
import pl.piomin.services.order.domain.event.OrderDelivered;

/** Completes the delivery and marks its order delivered in the same transaction. */
@Component
public class CompleteDeliveryHandler {

    @CommandHandler
    public void handle(CompleteDeliveryCommand command, @InjectEntity Delivery delivery, ProcessingContext context,
                       EventAppender eventAppender) {
        DeliveryCompleted completed = delivery.complete();
        OrderDelivered delivered = Orders.load(delivery.orderId(), context).deliver(delivery.id());
        eventAppender.append(completed, delivered);
    }
}
