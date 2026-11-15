package pl.piomin.services.delivery.command;

import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.UUID;

public record CompleteDeliveryCommand(@TargetEntityId UUID deliveryId) {
}
