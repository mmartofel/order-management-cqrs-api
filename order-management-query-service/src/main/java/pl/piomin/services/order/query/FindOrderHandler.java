package pl.piomin.services.order.query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.piomin.services.api.Reference;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.delivery.query.DeliveryResponse;
import pl.piomin.services.delivery.query.DeliveryView;
import pl.piomin.services.delivery.query.DeliveryViewRepository;
import pl.piomin.services.payment.query.PaymentResponse;
import pl.piomin.services.payment.query.PaymentViewRepository;
import pl.piomin.services.reference.ReferenceLabels;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
@Transactional(readOnly = true)
public class FindOrderHandler {

    private final OrderViewRepository orders;
    private final PaymentViewRepository payments;
    private final DeliveryViewRepository deliveries;
    private final ReferenceLabels labels;

    public FindOrderHandler(OrderViewRepository orders, PaymentViewRepository payments,
                            DeliveryViewRepository deliveries, ReferenceLabels labels) {
        this.orders = orders;
        this.payments = payments;
        this.deliveries = deliveries;
        this.labels = labels;
    }

    public OrderResponse handle(FindOrderQuery query) {
        OrderView order = orders.findById(query.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order " + query.orderId() + " not found"));
        List<DeliveryView> orderDeliveries = deliveries.findByOrderIdOrderByUpdatedAtAsc(order.getId());
        List<Long> addressIds = new ArrayList<>(orderDeliveries.stream().map(DeliveryView::getAddressId).toList());
        addressIds.add(order.getAddressId());
        Function<Long, Reference> addresses = labels.addresses(addressIds);
        boolean canWrite = SecurityConfig.canWrite();
        return OrderResponse.of(order, labels.persons(List.of(order.getPersonId())).apply(order.getPersonId()),
                addresses.apply(order.getAddressId()),
                payments.findByOrderIdOrderByUpdatedAtAsc(order.getId()).stream()
                        .map(p -> PaymentResponse.of(p, order.getStatus(), canWrite)).toList(),
                orderDeliveries.stream().map(d -> DeliveryResponse.of(d, addresses.apply(d.getAddressId()),
                        order.getStatus(), canWrite)).toList(),
                canWrite);
    }
}
