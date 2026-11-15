package pl.piomin.services.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Replaces the whole address; {@code version} is the one last read (409 on mismatch). */
public record AddressUpdateRequest(
        @NotNull Long personId,
        @NotBlank @Size(min = 1, max = 200) String street,
        @NotBlank @Size(min = 1, max = 100) String city,
        @NotBlank @Size(min = 1, max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String country,
        @NotNull Long version) {
}
