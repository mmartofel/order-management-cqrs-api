package pl.piomin.services.delivery.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.delivery.domain.Delivery;

/** Cancelling a delivery leaves the order PAID, so a new delivery can be scheduled. */
@Component
public class CancelDeliveryHandler {

    /** @return the delivery's version after the command */
    @CommandHandler
    public long handle(CancelDeliveryCommand command, @InjectEntity Delivery delivery, EventAppender eventAppender) {
        delivery.requireVersion(command.version());
        // computed before the append: Axon applies the appended event to the entity right away
        long next = command.version() + 1;
        eventAppender.append(delivery.cancel(command.reason()));
        return next;
    }
}
