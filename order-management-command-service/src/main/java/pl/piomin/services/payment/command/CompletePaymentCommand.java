package pl.piomin.services.payment.command;

import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.UUID;

/** {@code version} is the version the client saw; a different current version is a 409. */
public record CompletePaymentCommand(@TargetEntityId UUID paymentId, String transactionReference, long version) {
}
