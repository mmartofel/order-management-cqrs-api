package pl.piomin.services.order.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @NotNull Long personId,
        @NotNull Long addressId,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull Line> lines) {

    public record Line(@NotNull Long itemId, @NotNull @Min(1) @Max(1000) Integer quantity) {
    }
}
