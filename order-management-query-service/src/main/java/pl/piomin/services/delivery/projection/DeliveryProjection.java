package pl.piomin.services.delivery.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.config.EventRecord;
import pl.piomin.services.delivery.domain.event.DeliveryCancelled;
import pl.piomin.services.delivery.domain.event.DeliveryCompleted;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import pl.piomin.services.delivery.domain.event.DeliveryScheduled;
import pl.piomin.services.delivery.domain.event.DeliveryShipped;
import pl.piomin.services.delivery.query.DeliveryStatus;
import pl.piomin.services.delivery.query.DeliveryView;
import pl.piomin.services.delivery.query.DeliveryViewRepository;

/** Keeps {@code delivery_view} in step with the delivery events; idempotent by the event's sequence number. */
@Component
public class DeliveryProjection {

    private static final Logger log = LoggerFactory.getLogger(DeliveryProjection.class);

    private final DeliveryViewRepository repository;

    public DeliveryProjection(DeliveryViewRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apply(DeliveryEvent event, EventRecord record) {
        if (event instanceof DeliveryScheduled scheduled) {
            if (repository.existsById(scheduled.deliveryId())) {
                skipped(record);
                return;
            }
            repository.save(new DeliveryView(scheduled.deliveryId(), scheduled.orderId(), scheduled.addressId(),
                    scheduled.carrier(), scheduled.scheduledFor(), record.occurredAt(), record.sequence()));
            return;
        }
        DeliveryView view = repository.findById(event.deliveryId())
                .orElseThrow(() -> new IllegalStateException("Delivery " + event.deliveryId() + " is not projected yet"));
        if (record.sequence() <= view.getVersion()) {
            skipped(record);
            return;
        }
        switch (event) {
            case DeliveryShipped e -> {
                view.setTrackingNumber(e.trackingNumber());
                view.changed(DeliveryStatus.SHIPPED, record.occurredAt(), record.sequence());
            }
            case DeliveryCompleted e -> view.changed(DeliveryStatus.DELIVERED, record.occurredAt(), record.sequence());
            case DeliveryCancelled e -> {
                view.setCancelReason(e.reason());
                view.changed(DeliveryStatus.CANCELLED, record.occurredAt(), record.sequence());
            }
            case DeliveryScheduled e -> throw new IllegalStateException("handled above");
        }
    }

    private static void skipped(EventRecord record) {
        log.atDebug().addKeyValue("eventId", record.eventId()).addKeyValue("sequence", record.sequence())
                .log("Delivery event already applied, skipped");
    }
}
