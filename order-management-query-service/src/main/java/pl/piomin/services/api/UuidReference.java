package pl.piomin.services.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Summary of a related order embedded in a payment or delivery; orders have UUIDs, hence a second record. */
public record UuidReference(@Schema(requiredMode = REQUIRED) UUID id,
                            @Schema(requiredMode = REQUIRED, description = "Display name") String label) {

    public static UuidReference order(UUID id) {
        return new UuidReference(id, "Order " + id.toString().substring(0, 8));
    }
}
