package pl.piomin.services.order.domain.event;

import java.util.UUID;

public record OrderCancelled(UUID orderId, String reason) implements OrderEvent {
}
