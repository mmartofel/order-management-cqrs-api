package pl.piomin.services.order.query;

import pl.piomin.services.delivery.query.DeliveryResponse;
import pl.piomin.services.payment.query.PaymentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One order with its lines, payments and deliveries. {@code version} is the sequence number of its last event. */
public record OrderResponse(UUID id, Long personId, Long addressId, OrderStatus status, BigDecimal total,
                            Instant placedAt, Instant updatedAt, String cancelReason, List<Line> lines,
                            List<PaymentResponse> payments, List<DeliveryResponse> deliveries, long version) {

    public record Line(Long itemId, String sku, String name, int quantity, BigDecimal unitPrice) {
    }

    static OrderResponse of(OrderView view, List<PaymentResponse> payments, List<DeliveryResponse> deliveries) {
        return new OrderResponse(view.getId(), view.getPersonId(), view.getAddressId(), view.getStatus(),
                view.getTotal(), view.getPlacedAt(), view.getUpdatedAt(), view.getCancelReason(),
                view.getLines().stream()
                        .map(l -> new Line(l.getItemId(), l.getSku(), l.getName(), l.getQuantity(), l.getUnitPrice()))
                        .toList(),
                payments, deliveries, view.getVersion());
    }
}
