package pl.piomin.services.delivery.domain.event;

import java.util.UUID;

public record DeliveryCompleted(UUID deliveryId, UUID orderId) implements DeliveryEvent {
}
