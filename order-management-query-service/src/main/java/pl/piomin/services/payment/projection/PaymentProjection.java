package pl.piomin.services.payment.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.config.EventRecord;
import pl.piomin.services.payment.domain.event.PaymentCompleted;
import pl.piomin.services.payment.domain.event.PaymentEvent;
import pl.piomin.services.payment.domain.event.PaymentFailed;
import pl.piomin.services.payment.domain.event.PaymentInitiated;
import pl.piomin.services.payment.domain.event.PaymentRefunded;
import pl.piomin.services.payment.query.PaymentStatus;
import pl.piomin.services.payment.query.PaymentView;
import pl.piomin.services.payment.query.PaymentViewRepository;

/** Keeps {@code payment_view} in step with the payment events; idempotent by the event's sequence number. */
@Component
public class PaymentProjection {

    private static final Logger log = LoggerFactory.getLogger(PaymentProjection.class);

    private final PaymentViewRepository repository;

    public PaymentProjection(PaymentViewRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apply(PaymentEvent event, EventRecord record) {
        if (event instanceof PaymentInitiated initiated) {
            if (repository.existsById(initiated.paymentId())) {
                skipped(record);
                return;
            }
            repository.save(new PaymentView(initiated.paymentId(), initiated.orderId(), initiated.amount(),
                    initiated.method(), record.occurredAt(), record.sequence()));
            return;
        }
        PaymentView view = repository.findById(event.paymentId())
                .orElseThrow(() -> new IllegalStateException("Payment " + event.paymentId() + " is not projected yet"));
        if (record.sequence() <= view.getVersion()) {
            skipped(record);
            return;
        }
        switch (event) {
            case PaymentCompleted e -> {
                view.setTransactionReference(e.transactionReference());
                view.changed(PaymentStatus.COMPLETED, record.occurredAt(), record.sequence());
            }
            case PaymentFailed e -> {
                view.setFailureReason(e.reason());
                view.changed(PaymentStatus.FAILED, record.occurredAt(), record.sequence());
            }
            case PaymentRefunded e -> {
                view.setRefundReason(e.reason());
                view.changed(PaymentStatus.REFUNDED, record.occurredAt(), record.sequence());
            }
            case PaymentInitiated e -> throw new IllegalStateException("handled above");
        }
    }

    private static void skipped(EventRecord record) {
        log.atDebug().addKeyValue("eventId", record.eventId()).addKeyValue("sequence", record.sequence())
                .log("Payment event already applied, skipped");
    }
}
