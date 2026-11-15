package pl.piomin.services.order.query;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.piomin.services.api.Reference;
import pl.piomin.services.delivery.query.DeliveryResponse;
import pl.piomin.services.order.domain.OrderActions;
import pl.piomin.services.order.domain.OrderStatus;
import pl.piomin.services.payment.query.PaymentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** One order with its lines, payments and deliveries. */
public record OrderResponse(@Schema(requiredMode = REQUIRED) UUID id,
                            @Schema(requiredMode = REQUIRED) Reference person,
                            @Schema(requiredMode = REQUIRED) Reference address,
                            @Schema(requiredMode = REQUIRED) OrderStatus status,
                            @Schema(requiredMode = REQUIRED) BigDecimal total,
                            @Schema(requiredMode = REQUIRED) Instant placedAt,
                            @Schema(requiredMode = REQUIRED) Instant updatedAt,
                            String cancelReason,
                            @Schema(requiredMode = REQUIRED) List<OrderLineResponse> lines,
                            @Schema(requiredMode = REQUIRED) List<PaymentResponse> payments,
                            @Schema(requiredMode = REQUIRED) List<DeliveryResponse> deliveries,
                            @Schema(requiredMode = REQUIRED, description = "Actions the user may run now")
                            List<String> allowedActions,
                            @Schema(requiredMode = REQUIRED, description = "Version of the last applied event")
                            long version) {

    static OrderResponse of(OrderView view, Reference person, Reference address, List<PaymentResponse> payments,
                            List<DeliveryResponse> deliveries, boolean canWrite) {
        return new OrderResponse(view.getId(), person, address, view.getStatus(), view.getTotal(),
                view.getPlacedAt(), view.getUpdatedAt(), view.getCancelReason(),
                view.getLines().stream().map(OrderLineResponse::of).toList(), payments, deliveries,
                canWrite ? OrderActions.allowed(view.getStatus()) : List.of(), view.getVersion());
    }
}
