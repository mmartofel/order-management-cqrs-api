package pl.piomin.services.payment.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code version} is the sequence number of the payment's last applied event. */
public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount, String method, PaymentStatus status,
                              String transactionReference, String failureReason, String refundReason,
                              Instant updatedAt, long version) {

    public static PaymentResponse of(PaymentView view) {
        return new PaymentResponse(view.getId(), view.getOrderId(), view.getAmount(), view.getMethod(),
                view.getStatus(), view.getTransactionReference(), view.getFailureReason(), view.getRefundReason(),
                view.getUpdatedAt(), view.getVersion());
    }
}
