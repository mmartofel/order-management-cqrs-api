package pl.piomin.services.payment.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of the fail and refund commands. */
public record PaymentReasonRequest(@NotBlank @Size(min = 1, max = 500) String reason) {
}
