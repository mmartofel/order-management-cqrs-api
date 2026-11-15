package pl.piomin.services.order.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** References the person and address by id only, so the event holds no personal data. */
public record OrderPlaced(UUID orderId, Long personId, Long addressId, List<OrderLine> lines, BigDecimal total,
                          Instant placedAt) implements OrderEvent {

    public OrderPlaced {
        lines = List.copyOf(lines);
    }
}
