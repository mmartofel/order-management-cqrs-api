package pl.piomin.services.order.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(@NotBlank @Size(min = 1, max = 500) String reason) {
}
