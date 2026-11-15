package pl.piomin.services.payment.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record FailPaymentRequest(@NotBlank @Size(min = 1, max = 500) String reason,
                                 @Schema(requiredMode = REQUIRED, description = "Version from the last read")
                                 @NotNull Long version) {
}
