package pl.piomin.services.order.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** An order in a list, without its lines. */
public record OrderSummaryResponse(UUID id, Long personId, OrderStatus status, BigDecimal total, Instant placedAt,
                                   Instant updatedAt, long version) {

    static OrderSummaryResponse of(OrderView view) {
        return new OrderSummaryResponse(view.getId(), view.getPersonId(), view.getStatus(), view.getTotal(),
                view.getPlacedAt(), view.getUpdatedAt(), view.getVersion());
    }
}
