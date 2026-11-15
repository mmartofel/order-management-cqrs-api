package pl.piomin.services.payment.domain.event;

import java.util.UUID;

public record PaymentRefunded(UUID paymentId, UUID orderId, String reason) implements PaymentEvent {
}
