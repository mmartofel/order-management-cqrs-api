package pl.piomin.services.payment.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code method} is a payment method name such as CARD or TRANSFER, never card or account details. */
public record InitiatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "^(CARD|TRANSFER|BLIK|CASH_ON_DELIVERY)$") String method) {
}
