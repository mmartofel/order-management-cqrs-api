package pl.piomin.services.order.query;

import org.springframework.stereotype.Component;
import pl.piomin.services.order.domain.OrderStatus;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** The read model's status of each order, for the allowed actions of payments and deliveries that depend on it. */
@Component
public class OrderStatuses {

    private final OrderViewRepository repository;

    public OrderStatuses(OrderViewRepository repository) {
        this.repository = repository;
    }

    /** Orders that are not projected yet are missing from the map. */
    public Map<UUID, OrderStatus> of(Collection<UUID> orderIds) {
        return repository.findAllById(orderIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(OrderView::getId, OrderView::getStatus));
    }
}
