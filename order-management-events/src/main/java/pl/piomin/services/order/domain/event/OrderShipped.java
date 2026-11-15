package pl.piomin.services.order.domain.event;

import java.util.UUID;

public record OrderShipped(UUID orderId, UUID deliveryId) implements OrderEvent {
}
