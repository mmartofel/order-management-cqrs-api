package pl.piomin.services.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code country} is an ISO 3166-1 alpha-2 code, for example PL. */
public record AddressCreateRequest(
        @NotNull Long personId,
        @NotBlank @Size(min = 1, max = 200) String street,
        @NotBlank @Size(min = 1, max = 100) String city,
        @NotBlank @Size(min = 1, max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String country) {
}
