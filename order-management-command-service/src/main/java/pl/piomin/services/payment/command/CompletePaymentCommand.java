package pl.piomin.services.payment.command;

import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.UUID;

public record CompletePaymentCommand(@TargetEntityId UUID paymentId, String transactionReference) {
}
