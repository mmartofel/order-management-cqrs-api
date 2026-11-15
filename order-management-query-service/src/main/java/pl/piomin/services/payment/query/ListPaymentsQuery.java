package pl.piomin.services.payment.query;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** {@code orderId} is optional; without it all payments are listed. */
public record ListPaymentsQuery(UUID orderId, Pageable pageable) {
}
