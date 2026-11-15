package pl.piomin.services.delivery.command;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Schedules a delivery of a paid order, to the order's address. */
public record DeliveryCreateRequest(
        @NotNull UUID orderId,
        @NotBlank @Size(min = 1, max = 100) String carrier,
        @NotNull @FutureOrPresent LocalDate scheduledFor) {
}
