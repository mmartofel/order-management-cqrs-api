package pl.piomin.services.api;

import java.util.UUID;

/** What a command endpoint returns: the id of the aggregate the command was applied to. */
public record CommandResult(UUID id) {
}
