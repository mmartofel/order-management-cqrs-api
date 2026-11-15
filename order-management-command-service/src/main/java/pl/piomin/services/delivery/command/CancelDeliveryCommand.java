package pl.piomin.services.delivery.command;

import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.UUID;

/** {@code version} is the version the client saw; a different current version is a 409. */
public record CancelDeliveryCommand(@TargetEntityId UUID deliveryId, String reason, long version) {
}
