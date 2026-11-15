package pl.piomin.services.payment.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.order.domain.OrderStatus;
import pl.piomin.services.order.query.OrderStatuses;

import java.util.Map;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListPaymentsHandler {

    private final PaymentViewRepository repository;
    private final OrderStatuses orderStatuses;

    public ListPaymentsHandler(PaymentViewRepository repository, OrderStatuses orderStatuses) {
        this.repository = repository;
        this.orderStatuses = orderStatuses;
    }

    public Page<PaymentResponse> handle(PaymentFilter filter, Pageable pageable) {
        Page<PaymentView> page = repository.findAll(filter.toSpecification(), pageable);
        Map<UUID, OrderStatus> statuses = orderStatuses.of(page.map(PaymentView::getOrderId).toList());
        boolean canWrite = SecurityConfig.canWrite();
        return page.map(view -> PaymentResponse.of(view, statuses.get(view.getOrderId()), canWrite));
    }
}
