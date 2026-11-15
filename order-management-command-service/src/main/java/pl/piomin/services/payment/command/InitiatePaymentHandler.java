package pl.piomin.services.payment.command;

import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.springframework.stereotype.Component;
import pl.piomin.services.order.domain.Order;
import pl.piomin.services.order.domain.Orders;
import pl.piomin.services.payment.domain.event.PaymentInitiated;

/** A payment can be started only for a placed order, and only for its full total. */
@Component
public class InitiatePaymentHandler {

    /** @return the new aggregate's version, 0 */
    @CommandHandler
    public long handle(InitiatePaymentCommand command, ProcessingContext context, EventAppender eventAppender) {
        Order order = Orders.load(command.orderId(), context);
        order.acceptsPayment(command.amount());
        eventAppender.append(new PaymentInitiated(command.paymentId(), command.orderId(), command.amount(),
                command.method()));
        return 0L;
    }
}
