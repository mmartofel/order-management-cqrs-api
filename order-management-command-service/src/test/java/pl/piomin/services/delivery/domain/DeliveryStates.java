package pl.piomin.services.delivery.domain;

import pl.piomin.services.delivery.domain.event.DeliveryCancelled;
import pl.piomin.services.delivery.domain.event.DeliveryCompleted;
import pl.piomin.services.delivery.domain.event.DeliveryScheduled;
import pl.piomin.services.delivery.domain.event.DeliveryShipped;

import java.time.LocalDate;
import java.util.UUID;

/** Builds a delivery in a given status the way Axon does: by applying its events. */
public final class DeliveryStates {

    private DeliveryStates() {
    }

    public static Delivery in(DeliveryStatus status) {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.on(new DeliveryScheduled(id, orderId, 2L, "DHL", LocalDate.now().plusDays(1)));
        switch (status) {
            case SCHEDULED -> {
            }
            case SHIPPED -> delivery.on(new DeliveryShipped(id, orderId, "TRACK-1"));
            case DELIVERED -> {
                delivery.on(new DeliveryShipped(id, orderId, "TRACK-1"));
                delivery.on(new DeliveryCompleted(id, orderId));
            }
            case CANCELLED -> delivery.on(new DeliveryCancelled(id, orderId, "test"));
        }
        return delivery;
    }
}
