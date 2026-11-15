package pl.piomin.services.order.domain.event;

import java.util.UUID;

public record OrderPaid(UUID orderId, UUID paymentId) implements OrderEvent {
}
