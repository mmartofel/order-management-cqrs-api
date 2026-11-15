package pl.piomin.services.payment.command;

import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.UUID;

/** {@code version} is the version the client saw; a different current version is a 409. */
public record FailPaymentCommand(@TargetEntityId UUID paymentId, String reason, long version) {
}
