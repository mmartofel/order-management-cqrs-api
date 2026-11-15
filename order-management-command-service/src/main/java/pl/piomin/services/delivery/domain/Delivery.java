package pl.piomin.services.delivery.domain;

import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import pl.piomin.services.delivery.domain.event.DeliveryCancelled;
import pl.piomin.services.delivery.domain.event.DeliveryCompleted;
import pl.piomin.services.delivery.domain.event.DeliveryEvent;
import pl.piomin.services.delivery.domain.event.DeliveryScheduled;
import pl.piomin.services.delivery.domain.event.DeliveryShipped;
import pl.piomin.services.exception.BusinessRuleViolationException;

import java.util.UUID;

/** Event-sourced Delivery: SCHEDULED, SHIPPED, DELIVERED; only a SCHEDULED delivery can be CANCELLED. */
@EventSourced(tagKey = DeliveryEvent.AGGREGATE_TYPE, idType = UUID.class)
public class Delivery {

    private UUID id;
    private UUID orderId;
    private DeliveryStatus status;

    @EntityCreator
    public Delivery() {
    }

    public DeliveryShipped ship(String trackingNumber) {
        requireStatus(DeliveryStatus.SCHEDULED, "shipped");
        return new DeliveryShipped(id, orderId, trackingNumber);
    }

    public DeliveryCompleted complete() {
        requireStatus(DeliveryStatus.SHIPPED, "completed");
        return new DeliveryCompleted(id, orderId);
    }

    public DeliveryCancelled cancel(String reason) {
        requireStatus(DeliveryStatus.SCHEDULED, "cancelled");
        return new DeliveryCancelled(id, orderId, reason);
    }

    private void requireStatus(DeliveryStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleViolationException(
                    "Delivery " + id + " is " + status + " and cannot be " + action + "; expected " + expected);
        }
    }

    @EventSourcingHandler
    void on(DeliveryScheduled event) {
        this.id = event.deliveryId();
        this.orderId = event.orderId();
        this.status = DeliveryStatus.SCHEDULED;
    }

    @EventSourcingHandler
    void on(DeliveryShipped event) {
        this.status = DeliveryStatus.SHIPPED;
    }

    @EventSourcingHandler
    void on(DeliveryCompleted event) {
        this.status = DeliveryStatus.DELIVERED;
    }

    @EventSourcingHandler
    void on(DeliveryCancelled event) {
        this.status = DeliveryStatus.CANCELLED;
    }

    public UUID id() {
        return id;
    }

    public UUID orderId() {
        return orderId;
    }

    public DeliveryStatus status() {
        return status;
    }
}
