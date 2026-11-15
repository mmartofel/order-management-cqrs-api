package pl.piomin.services.api;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Summary of a related record of the reference service (person, address) embedded in a response. The same record as
 * in the reference service, so the merged contract has one {@code Reference}.
 */
public record Reference(@Schema(requiredMode = REQUIRED) Long id,
                        @Schema(requiredMode = REQUIRED, description = "Display name") String label) {
}
