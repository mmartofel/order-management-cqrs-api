package pl.piomin.services.person;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record PersonResponse(@Schema(requiredMode = REQUIRED) Long id,
                             @Schema(requiredMode = REQUIRED) String firstName,
                             @Schema(requiredMode = REQUIRED) String lastName,
                             @Schema(requiredMode = REQUIRED) String email,
                             String phone,
                             @Schema(requiredMode = REQUIRED) Long version) {

    static PersonResponse of(Person person) {
        return new PersonResponse(person.getId(), person.getFirstName(), person.getLastName(), person.getEmail(),
                person.getPhone(), person.getVersion());
    }
}
