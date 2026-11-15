package pl.piomin.services.order.command;

import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.UUID;

public record CancelOrderCommand(@TargetEntityId UUID orderId, String reason) {
}
