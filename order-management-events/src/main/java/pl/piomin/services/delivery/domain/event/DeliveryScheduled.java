package pl.piomin.services.delivery.domain.event;

import java.time.LocalDate;
import java.util.UUID;

/** References the delivery address by id only, so the event holds no personal data. */
public record DeliveryScheduled(UUID deliveryId, UUID orderId, Long addressId, String carrier, LocalDate scheduledFor)
        implements DeliveryEvent {
}
