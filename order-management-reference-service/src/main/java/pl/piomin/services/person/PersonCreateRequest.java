package pl.piomin.services.person;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PersonCreateRequest(
        @NotBlank @Size(min = 1, max = 100) String firstName,
        @NotBlank @Size(min = 1, max = 100) String lastName,
        @NotBlank @Email @Size(min = 1, max = 255) String email,
        @Size(max = 30) @Pattern(regexp = "^\\+?[0-9 ()-]*$") String phone) {
}
