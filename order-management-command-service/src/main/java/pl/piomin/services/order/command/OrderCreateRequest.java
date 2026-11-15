package pl.piomin.services.order.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Places an order; the same item listed twice becomes one line with the summed quantity. */
public record OrderCreateRequest(
        @NotNull Long personId,
        @NotNull Long addressId,
        @NotEmpty @Size(min = 1, max = 100) List<@Valid @NotNull OrderLineRequest> lines) {

    public record OrderLineRequest(@NotNull Long itemId, @NotNull @Min(1) @Max(1000) Integer quantity) {
    }
}
