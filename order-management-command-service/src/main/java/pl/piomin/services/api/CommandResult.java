package pl.piomin.services.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * What every command endpoint returns: the aggregate's id and its version after the command. The read model has
 * caught up with the command once its {@code version} is at least this one.
 */
public record CommandResult(@Schema(requiredMode = REQUIRED) UUID id,
                            @Schema(requiredMode = REQUIRED, description = "Version of the aggregate after the command")
                            long version) {
}
