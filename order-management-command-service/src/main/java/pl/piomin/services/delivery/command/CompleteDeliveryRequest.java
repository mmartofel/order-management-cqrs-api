package pl.piomin.services.delivery.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** The action has no input besides the version. */
public record CompleteDeliveryRequest(
        @Schema(requiredMode = REQUIRED, description = "Version from the last read") @NotNull Long version) {
}
