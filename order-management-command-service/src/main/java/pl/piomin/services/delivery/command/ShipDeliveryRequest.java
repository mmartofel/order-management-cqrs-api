package pl.piomin.services.delivery.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipDeliveryRequest(@NotBlank @Size(min = 1, max = 100) String trackingNumber) {
}
