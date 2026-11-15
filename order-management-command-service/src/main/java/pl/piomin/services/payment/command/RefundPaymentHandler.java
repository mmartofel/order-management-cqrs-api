package pl.piomin.services.payment.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.stereotype.Component;
import pl.piomin.services.exception.BusinessRuleViolationException;
import pl.piomin.services.order.domain.Order;
import pl.piomin.services.order.domain.OrderStatus;
import pl.piomin.services.order.domain.Orders;
import pl.piomin.services.payment.domain.Payment;
import pl.piomin.services.payment.domain.event.PaymentRefunded;

/** Only the payment of a cancelled order is refunded. */
@Component
public class RefundPaymentHandler {

    @CommandHandler
    public void handle(RefundPaymentCommand command, @InjectEntity Payment payment, ProcessingContext context,
                       EventAppender eventAppender) {
        PaymentRefunded refunded = payment.refund(command.reason());
        Order order = Orders.load(payment.orderId(), context);
        if (order.status() != OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Order " + order.id() + " is " + order.status() + "; only a cancelled order is refunded");
        }
        eventAppender.append(refunded);
    }
}
