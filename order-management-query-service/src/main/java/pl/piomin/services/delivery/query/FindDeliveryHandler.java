package pl.piomin.services.delivery.query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.order.query.OrderStatuses;
import pl.piomin.services.reference.ReferenceLabels;

import java.util.List;

@Component
@Transactional(readOnly = true)
public class FindDeliveryHandler {

    private final DeliveryViewRepository repository;
    private final OrderStatuses orderStatuses;
    private final ReferenceLabels labels;

    public FindDeliveryHandler(DeliveryViewRepository repository, OrderStatuses orderStatuses,
                               ReferenceLabels labels) {
        this.repository = repository;
        this.orderStatuses = orderStatuses;
        this.labels = labels;
    }

    public DeliveryResponse handle(FindDeliveryQuery query) {
        DeliveryView view = repository.findById(query.deliveryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Delivery " + query.deliveryId() + " not found"));
        return DeliveryResponse.of(view, labels.addresses(List.of(view.getAddressId())).apply(view.getAddressId()),
                orderStatuses.of(List.of(view.getOrderId())).get(view.getOrderId()), SecurityConfig.canWrite());
    }
}
