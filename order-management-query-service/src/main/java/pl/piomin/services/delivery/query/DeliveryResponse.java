package pl.piomin.services.delivery.query;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.piomin.services.api.Reference;
import pl.piomin.services.api.UuidReference;
import pl.piomin.services.delivery.domain.DeliveryActions;
import pl.piomin.services.delivery.domain.DeliveryStatus;
import pl.piomin.services.order.domain.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record DeliveryResponse(@Schema(requiredMode = REQUIRED) UUID id,
                               @Schema(requiredMode = REQUIRED) UuidReference order,
                               @Schema(requiredMode = REQUIRED) Reference address,
                               @Schema(requiredMode = REQUIRED) String carrier,
                               @Schema(requiredMode = REQUIRED) LocalDate scheduledFor,
                               @Schema(requiredMode = REQUIRED) DeliveryStatus status,
                               String trackingNumber,
                               String cancelReason,
                               @Schema(requiredMode = REQUIRED) Instant updatedAt,
                               @Schema(requiredMode = REQUIRED, description = "Actions the user may run now")
                               List<String> allowedActions,
                               @Schema(requiredMode = REQUIRED, description = "Version of the last applied event")
                               long version) {

    /** @param orderStatus the order's status in the read model, or {@code null} when it is not projected yet */
    public static DeliveryResponse of(DeliveryView view, Reference address, OrderStatus orderStatus,
                                      boolean canWrite) {
        return new DeliveryResponse(view.getId(), UuidReference.order(view.getOrderId()), address, view.getCarrier(),
                view.getScheduledFor(), view.getStatus(), view.getTrackingNumber(), view.getCancelReason(),
                view.getUpdatedAt(), canWrite ? DeliveryActions.allowed(view.getStatus(), orderStatus) : List.of(),
                view.getVersion());
    }
}
