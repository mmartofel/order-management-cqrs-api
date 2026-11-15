package pl.piomin.services.delivery.query;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** {@code orderId} is optional; without it all deliveries are listed. */
public record ListDeliveriesQuery(UUID orderId, Pageable pageable) {
}
