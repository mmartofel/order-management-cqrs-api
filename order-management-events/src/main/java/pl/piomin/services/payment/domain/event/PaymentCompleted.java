package pl.piomin.services.payment.domain.event;

import java.util.UUID;

public record PaymentCompleted(UUID paymentId, UUID orderId, String transactionReference) implements PaymentEvent {
}
