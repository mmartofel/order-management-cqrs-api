package pl.piomin.services.payment.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code method} is the payment method name (for example CARD or TRANSFER), never card or account details. */
public record PaymentInitiated(UUID paymentId, UUID orderId, BigDecimal amount, String method)
        implements PaymentEvent {
}
