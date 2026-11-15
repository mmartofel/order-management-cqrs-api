package pl.piomin.services.person;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Replaces the whole person; {@code version} is the one last read (409 on mismatch). */
public record PersonUpdateRequest(
        @NotBlank @Size(min = 1, max = 100) String firstName,
        @NotBlank @Size(min = 1, max = 100) String lastName,
        @NotBlank @Email @Size(min = 1, max = 255) String email,
        @Size(max = 30) @Pattern(regexp = "^\\+?[0-9 ()-]*$") String phone,
        @NotNull Long version) {
}
