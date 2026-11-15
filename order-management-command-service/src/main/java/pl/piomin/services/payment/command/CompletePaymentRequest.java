package pl.piomin.services.payment.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePaymentRequest(@NotBlank @Size(min = 1, max = 100) String transactionReference) {
}
