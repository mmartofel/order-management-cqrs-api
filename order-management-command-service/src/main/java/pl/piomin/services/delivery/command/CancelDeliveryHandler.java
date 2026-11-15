package pl.piomin.services.delivery.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.delivery.domain.Delivery;

/** Cancelling a delivery leaves the order PAID, so a new delivery can be scheduled. */
@Component
public class CancelDeliveryHandler {

    @CommandHandler
    public void handle(CancelDeliveryCommand command, @InjectEntity Delivery delivery, EventAppender eventAppender) {
        eventAppender.append(delivery.cancel(command.reason()));
    }
}
