package pl.piomin.services.payment.domain;

import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import pl.piomin.services.exception.BusinessRuleViolationException;
import pl.piomin.services.exception.StaleVersionException;
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
    /** Sequence number of the last event applied: 0 after the creating event, as in the event store. */
    private long version = -1;

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
        this.version++;
        this.id = event.paymentId();
        this.orderId = event.orderId();
        this.status = PaymentStatus.INITIATED;
    }

    @EventSourcingHandler
    void on(PaymentCompleted event) {
        this.version++;
        this.status = PaymentStatus.COMPLETED;
    }

    @EventSourcingHandler
    void on(PaymentFailed event) {
        this.version++;
        this.status = PaymentStatus.FAILED;
    }

    @EventSourcingHandler
    void on(PaymentRefunded event) {
        this.version++;
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

    public long version() {
        return version;
    }

    /** 409 without field errors when the client did not see the current version: it has to reload first. */
    public void requireVersion(long expected) {
        if (expected != version) {
            throw new StaleVersionException("Payment " + id + " is at version " + version + ", not " + expected);
        }
    }
}
