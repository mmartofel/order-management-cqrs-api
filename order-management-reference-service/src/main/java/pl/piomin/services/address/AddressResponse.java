package pl.piomin.services.address;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.piomin.services.api.Reference;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** {@code person} is the owner as {@code {id, label}}, so a list of addresses needs no request per row. */
public record AddressResponse(@Schema(requiredMode = REQUIRED) Long id,
                              @Schema(requiredMode = REQUIRED) Reference person,
                              @Schema(requiredMode = REQUIRED) String street,
                              @Schema(requiredMode = REQUIRED) String city,
                              @Schema(requiredMode = REQUIRED) String postalCode,
                              @Schema(requiredMode = REQUIRED) String country,
                              @Schema(requiredMode = REQUIRED) Long version) {

    static AddressResponse of(Address address, Reference person) {
        return new AddressResponse(address.getId(), person, address.getStreet(), address.getCity(),
                address.getPostalCode(), address.getCountry(), address.getVersion());
    }
}
