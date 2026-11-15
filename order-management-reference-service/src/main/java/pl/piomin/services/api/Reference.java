package pl.piomin.services.api;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Summary of a related record embedded in a response, so a list never needs one request per row. The same record as
 * in the query service, so the merged contract has one {@code Reference}.
 */
public record Reference(@Schema(requiredMode = REQUIRED) Long id,
                        @Schema(requiredMode = REQUIRED, description = "Display name") String label) {
}
