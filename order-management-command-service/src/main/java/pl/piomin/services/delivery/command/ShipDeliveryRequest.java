package pl.piomin.services.delivery.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ShipDeliveryRequest(@NotBlank @Size(min = 1, max = 100) String trackingNumber,
                                  @Schema(requiredMode = REQUIRED, description = "Version from the last read")
                                  @NotNull Long version) {
}
