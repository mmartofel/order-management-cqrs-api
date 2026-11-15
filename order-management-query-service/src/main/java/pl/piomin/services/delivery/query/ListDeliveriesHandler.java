package pl.piomin.services.delivery.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.api.Reference;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.order.domain.OrderStatus;
import pl.piomin.services.order.query.OrderStatuses;
import pl.piomin.services.reference.ReferenceLabels;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
@Transactional(readOnly = true)
public class ListDeliveriesHandler {

    private final DeliveryViewRepository repository;
    private final OrderStatuses orderStatuses;
    private final ReferenceLabels labels;

    public ListDeliveriesHandler(DeliveryViewRepository repository, OrderStatuses orderStatuses,
                                 ReferenceLabels labels) {
        this.repository = repository;
        this.orderStatuses = orderStatuses;
        this.labels = labels;
    }

    public Page<DeliveryResponse> handle(DeliveryFilter filter, Pageable pageable) {
        Page<DeliveryView> page = repository.findAll(filter.toSpecification(), pageable);
        Map<UUID, OrderStatus> statuses = orderStatuses.of(page.map(DeliveryView::getOrderId).toList());
        Function<Long, Reference> addresses = labels.addresses(page.map(DeliveryView::getAddressId).toList());
        boolean canWrite = SecurityConfig.canWrite();
        return page.map(view -> DeliveryResponse.of(view, addresses.apply(view.getAddressId()),
                statuses.get(view.getOrderId()), canWrite));
    }
}
