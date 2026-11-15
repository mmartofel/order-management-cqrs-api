package pl.piomin.services.payment.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.payment.domain.Payment;

/** A failed payment leaves the order PLACED, so a new payment can be initiated. */
@Component
public class FailPaymentHandler {

    @CommandHandler
    public void handle(FailPaymentCommand command, @InjectEntity Payment payment, EventAppender eventAppender) {
        eventAppender.append(payment.fail(command.reason()));
    }
}
