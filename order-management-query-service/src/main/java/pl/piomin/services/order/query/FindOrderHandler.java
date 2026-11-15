package pl.piomin.services.order.query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.piomin.services.delivery.query.DeliveryResponse;
import pl.piomin.services.delivery.query.DeliveryViewRepository;
import pl.piomin.services.payment.query.PaymentResponse;
import pl.piomin.services.payment.query.PaymentViewRepository;

@Component
@Transactional(readOnly = true)
public class FindOrderHandler {

    private final OrderViewRepository orders;
    private final PaymentViewRepository payments;
    private final DeliveryViewRepository deliveries;

    public FindOrderHandler(OrderViewRepository orders, PaymentViewRepository payments,
                            DeliveryViewRepository deliveries) {
        this.orders = orders;
        this.payments = payments;
        this.deliveries = deliveries;
    }

    public OrderResponse handle(FindOrderQuery query) {
        OrderView order = orders.findById(query.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order " + query.orderId() + " not found"));
        return OrderResponse.of(order,
                payments.findByOrderIdOrderByUpdatedAtAsc(order.getId()).stream().map(PaymentResponse::of).toList(),
                deliveries.findByOrderIdOrderByUpdatedAtAsc(order.getId()).stream().map(DeliveryResponse::of).toList());
    }
}
