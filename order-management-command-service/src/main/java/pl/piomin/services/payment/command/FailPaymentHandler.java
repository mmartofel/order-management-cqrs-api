package pl.piomin.services.payment.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.payment.domain.Payment;

/** A failed payment leaves the order PLACED, so a new payment can be initiated. */
@Component
public class FailPaymentHandler {

    /** @return the payment's version after the command */
    @CommandHandler
    public long handle(FailPaymentCommand command, @InjectEntity Payment payment, EventAppender eventAppender) {
        payment.requireVersion(command.version());
        // computed before the append: Axon applies the appended event to the entity right away
        long next = command.version() + 1;
        eventAppender.append(payment.fail(command.reason()));
        return next;
    }
}
