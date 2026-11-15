package pl.piomin.services.payment.domain;

import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import pl.piomin.services.exception.BusinessRuleViolationException;
import pl.piomin.services.payment.domain.event.PaymentCompleted;
import pl.piomin.services.payment.domain.event.PaymentEvent;
import pl.piomin.services.payment.domain.event.PaymentFailed;
import pl.piomin.services.payment.domain.event.PaymentInitiated;
import pl.piomin.services.payment.domain.event.PaymentRefunded;

import java.util.UUID;

/** Event-sourced Payment: INITIATED, then COMPLETED or FAILED; a COMPLETED payment can be REFUNDED. */
@EventSourced(tagKey = PaymentEvent.AGGREGATE_TYPE, idType = UUID.class)
public class Payment {

    private UUID id;
    private UUID orderId;
    private PaymentStatus status;

    @EntityCreator
    public Payment() {
    }

    public PaymentCompleted complete(String transactionReference) {
        requireStatus(PaymentStatus.INITIATED, "completed");
        return new PaymentCompleted(id, orderId, transactionReference);
    }

    public PaymentFailed fail(String reason) {
        requireStatus(PaymentStatus.INITIATED, "failed");
        return new PaymentFailed(id, orderId, reason);
    }

    public PaymentRefunded refund(String reason) {
        requireStatus(PaymentStatus.COMPLETED, "refunded");
        return new PaymentRefunded(id, orderId, reason);
    }

    private void requireStatus(PaymentStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleViolationException(
                    "Payment " + id + " is " + status + " and cannot be " + action + "; expected " + expected);
        }
    }

    @EventSourcingHandler
    void on(PaymentInitiated event) {
        this.id = event.paymentId();
        this.orderId = event.orderId();
        this.status = PaymentStatus.INITIATED;
    }

    @EventSourcingHandler
    void on(PaymentCompleted event) {
        this.status = PaymentStatus.COMPLETED;
    }

    @EventSourcingHandler
    void on(PaymentFailed event) {
        this.status = PaymentStatus.FAILED;
    }

    @EventSourcingHandler
    void on(PaymentRefunded event) {
        this.status = PaymentStatus.REFUNDED;
    }

    public UUID id() {
        return id;
    }

    public UUID orderId() {
        return orderId;
    }

    public PaymentStatus status() {
        return status;
    }
}
