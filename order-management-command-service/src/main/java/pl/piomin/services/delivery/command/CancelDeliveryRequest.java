package pl.piomin.services.delivery.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelDeliveryRequest(@NotBlank @Size(min = 1, max = 500) String reason) {
}
