package pl.piomin.services.payment.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.order.domain.Orders;
import pl.piomin.services.payment.domain.Payment;
import pl.piomin.services.payment.domain.event.PaymentRefunded;

/** Only the payment of a cancelled order is refunded. */
@Component
public class RefundPaymentHandler {

    /** @return the payment's version after the command */
    @CommandHandler
    public long handle(RefundPaymentCommand command, @InjectEntity Payment payment, ProcessingContext context,
                       EventAppender eventAppender) {
        payment.requireVersion(command.version());
        // computed before the append: Axon applies the appended event to the entity right away
        long next = command.version() + 1;
        PaymentRefunded refunded = payment.refund(command.reason());
        Orders.load(payment.orderId(), context).acceptsRefund();
        eventAppender.append(refunded);
        return next;
    }
}
