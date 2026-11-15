package pl.piomin.services.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Replaces the whole item; {@code version} is the one last read (409 on mismatch). */
public record ItemUpdateRequest(
        @NotBlank @Size(min = 1, max = 50) String sku,
        @NotBlank @Size(min = 1, max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull Long version) {
}
