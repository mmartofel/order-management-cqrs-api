package pl.piomin.services.order.query;

import org.springframework.data.domain.Pageable;

/** Both filters are optional. */
public record ListOrdersQuery(Long personId, OrderStatus status, Pageable pageable) {
}
