package pl.piomin.services.payment.query;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ListPaymentsHandler {

    private final PaymentViewRepository repository;

    public ListPaymentsHandler(PaymentViewRepository repository) {
        this.repository = repository;
    }

    public Page<PaymentResponse> handle(ListPaymentsQuery query) {
        Page<PaymentView> page = query.orderId() == null
                ? repository.findAll(query.pageable())
                : repository.findByOrderId(query.orderId(), query.pageable());
        return page.map(PaymentResponse::of);
    }
}
