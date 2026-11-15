package pl.piomin.services.order.domain.event;

import java.util.UUID;

public record OrderDelivered(UUID orderId, UUID deliveryId) implements OrderEvent {
}
