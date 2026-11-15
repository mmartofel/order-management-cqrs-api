package pl.piomin.services.order.query;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.piomin.services.api.Reference;
import pl.piomin.services.order.domain.OrderActions;
import pl.piomin.services.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** An order in a list, without its lines, payments and deliveries. */
public record OrderSummaryResponse(@Schema(requiredMode = REQUIRED) UUID id,
                                   @Schema(requiredMode = REQUIRED) Reference person,
                                   @Schema(requiredMode = REQUIRED) OrderStatus status,
                                   @Schema(requiredMode = REQUIRED) BigDecimal total,
                                   @Schema(requiredMode = REQUIRED) Instant placedAt,
                                   @Schema(requiredMode = REQUIRED) Instant updatedAt,
                                   @Schema(requiredMode = REQUIRED, description = "Actions the user may run now")
                                   List<String> allowedActions,
                                   @Schema(requiredMode = REQUIRED, description = "Version of the last applied event")
                                   long version) {

    static OrderSummaryResponse of(OrderView view, Reference person, boolean canWrite) {
        return new OrderSummaryResponse(view.getId(), person, view.getStatus(), view.getTotal(), view.getPlacedAt(),
                view.getUpdatedAt(), canWrite ? OrderActions.allowed(view.getStatus()) : List.of(), view.getVersion());
    }
}
