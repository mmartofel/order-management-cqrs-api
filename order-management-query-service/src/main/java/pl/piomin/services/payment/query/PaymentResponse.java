package pl.piomin.services.payment.query;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.piomin.services.api.UuidReference;
import pl.piomin.services.order.domain.OrderStatus;
import pl.piomin.services.payment.domain.PaymentActions;
import pl.piomin.services.payment.domain.PaymentMethod;
import pl.piomin.services.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record PaymentResponse(@Schema(requiredMode = REQUIRED) UUID id,
                              @Schema(requiredMode = REQUIRED) UuidReference order,
                              @Schema(requiredMode = REQUIRED) BigDecimal amount,
                              @Schema(requiredMode = REQUIRED) PaymentMethod method,
                              @Schema(requiredMode = REQUIRED) PaymentStatus status,
                              String transactionReference,
                              String failureReason,
                              String refundReason,
                              @Schema(requiredMode = REQUIRED) Instant updatedAt,
                              @Schema(requiredMode = REQUIRED, description = "Actions the user may run now")
                              List<String> allowedActions,
                              @Schema(requiredMode = REQUIRED, description = "Version of the last applied event")
                              long version) {

    /** @param orderStatus the order's status in the read model, or {@code null} when it is not projected yet */
    public static PaymentResponse of(PaymentView view, OrderStatus orderStatus, boolean canWrite) {
        return new PaymentResponse(view.getId(), UuidReference.order(view.getOrderId()), view.getAmount(),
                PaymentMethod.valueOf(view.getMethod()), view.getStatus(), view.getTransactionReference(),
                view.getFailureReason(), view.getRefundReason(), view.getUpdatedAt(),
                canWrite ? PaymentActions.allowed(view.getStatus(), orderStatus) : List.of(), view.getVersion());
    }
}
