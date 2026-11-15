package pl.piomin.services.order.query;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ListOrdersHandler {

    private final OrderViewRepository repository;

    public ListOrdersHandler(OrderViewRepository repository) {
        this.repository = repository;
    }

    public Page<OrderSummaryResponse> handle(ListOrdersQuery query) {
        Specification<OrderView> spec = Specification.unrestricted();
        if (query.personId() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("personId"), query.personId()));
        }
        if (query.status() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), query.status()));
        }
        return repository.findAll(spec, query.pageable()).map(OrderSummaryResponse::of);
    }
}
