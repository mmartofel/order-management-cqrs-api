package pl.piomin.services.order.command;

import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.UUID;

/** {@code version} is the order version the client saw; a different current version is a 409. */
public record CancelOrderCommand(@TargetEntityId UUID orderId, String reason, long version) {
}
