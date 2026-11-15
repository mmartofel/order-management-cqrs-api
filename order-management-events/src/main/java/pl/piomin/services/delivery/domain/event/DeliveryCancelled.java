package pl.piomin.services.delivery.domain.event;

import java.util.UUID;

public record DeliveryCancelled(UUID deliveryId, UUID orderId, String reason) implements DeliveryEvent {
}
