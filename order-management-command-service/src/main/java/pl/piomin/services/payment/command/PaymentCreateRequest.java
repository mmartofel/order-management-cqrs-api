package pl.piomin.services.payment.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import pl.piomin.services.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/** Starts a payment for the full total of a placed order. */
public record PaymentCreateRequest(
        @NotNull UUID orderId,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod method) {
}
