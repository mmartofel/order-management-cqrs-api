package pl.piomin.services.delivery.domain.event;

import java.util.UUID;

public record DeliveryShipped(UUID deliveryId, UUID orderId, String trackingNumber) implements DeliveryEvent {
}
