package pl.piomin.services.order.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.order.domain.Order;

@Component
public class CancelOrderHandler {

    /** @return the order's version after the command */
    @CommandHandler
    public long handle(CancelOrderCommand command, @InjectEntity Order order, EventAppender eventAppender) {
        order.requireVersion(command.version());
        // computed before the append: Axon applies the appended event to the entity right away
        long next = command.version() + 1;
        eventAppender.append(order.cancel(command.reason()));
        return next;
    }
}
