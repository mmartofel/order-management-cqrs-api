package pl.piomin.services.delivery.query;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ListDeliveriesHandler {

    private final DeliveryViewRepository repository;

    public ListDeliveriesHandler(DeliveryViewRepository repository) {
        this.repository = repository;
    }

    public Page<DeliveryResponse> handle(ListDeliveriesQuery query) {
        Page<DeliveryView> page = query.orderId() == null
                ? repository.findAll(query.pageable())
                : repository.findByOrderId(query.orderId(), query.pageable());
        return page.map(DeliveryResponse::of);
    }
}
