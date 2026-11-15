package pl.piomin.services.delivery.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** {@code version} is the sequence number of the delivery's last applied event. */
public record DeliveryResponse(UUID id, UUID orderId, Long addressId, String carrier, LocalDate scheduledFor,
                               DeliveryStatus status, String trackingNumber, String cancelReason, Instant updatedAt,
                               long version) {

    public static DeliveryResponse of(DeliveryView view) {
        return new DeliveryResponse(view.getId(), view.getOrderId(), view.getAddressId(), view.getCarrier(),
                view.getScheduledFor(), view.getStatus(), view.getTrackingNumber(), view.getCancelReason(),
                view.getUpdatedAt(), view.getVersion());
    }
}
