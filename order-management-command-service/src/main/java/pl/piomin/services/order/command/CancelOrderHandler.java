package pl.piomin.services.order.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.order.domain.Order;

@Component
public class CancelOrderHandler {

    @CommandHandler
    public void handle(CancelOrderCommand command, @InjectEntity Order order, EventAppender eventAppender) {
        eventAppender.append(order.cancel(command.reason()));
    }
}
