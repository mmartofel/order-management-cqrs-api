package pl.piomin.services.order.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.api.Reference;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.reference.ReferenceLabels;

import java.util.function.Function;

@Component
@Transactional(readOnly = true)
public class ListOrdersHandler {

    private final OrderViewRepository repository;
    private final ReferenceLabels labels;

    public ListOrdersHandler(OrderViewRepository repository, ReferenceLabels labels) {
        this.repository = repository;
        this.labels = labels;
    }

    public Page<OrderSummaryResponse> handle(OrderFilter filter, Pageable pageable) {
        Page<OrderView> page = repository.findAll(filter.toSpecification(), pageable);
        Function<Long, Reference> persons = labels.persons(page.map(OrderView::getPersonId).toList());
        boolean canWrite = SecurityConfig.canWrite();
        return page.map(view -> OrderSummaryResponse.of(view, persons.apply(view.getPersonId()), canWrite));
    }
}
