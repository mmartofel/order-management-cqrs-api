package pl.piomin.services.payment.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.order.domain.Order;
import pl.piomin.services.order.domain.Orders;
import pl.piomin.services.order.domain.event.OrderPaid;
import pl.piomin.services.payment.domain.Payment;
import pl.piomin.services.payment.domain.event.PaymentCompleted;

/** Completes the payment and marks its order paid in the same transaction. */
@Component
public class CompletePaymentHandler {

    /** @return the payment's version after the command */
    @CommandHandler
    public long handle(CompletePaymentCommand command, @InjectEntity Payment payment, ProcessingContext context,
                       EventAppender eventAppender) {
        payment.requireVersion(command.version());
        // computed before the append: Axon applies the appended event to the entity right away
        long next = command.version() + 1;
        PaymentCompleted completed = payment.complete(command.transactionReference());
        OrderPaid paid = Orders.load(payment.orderId(), context).pay(payment.id());
        eventAppender.append(completed, paid);
        return next;
    }
}
