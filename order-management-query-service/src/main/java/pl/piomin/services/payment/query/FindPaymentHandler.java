package pl.piomin.services.payment.query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.piomin.services.config.SecurityConfig;
import pl.piomin.services.order.query.OrderStatuses;

import java.util.List;

@Component
@Transactional(readOnly = true)
public class FindPaymentHandler {

    private final PaymentViewRepository repository;
    private final OrderStatuses orderStatuses;

    public FindPaymentHandler(PaymentViewRepository repository, OrderStatuses orderStatuses) {
        this.repository = repository;
        this.orderStatuses = orderStatuses;
    }

    public PaymentResponse handle(FindPaymentQuery query) {
        PaymentView view = repository.findById(query.paymentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment " + query.paymentId() + " not found"));
        return PaymentResponse.of(view, orderStatuses.of(List.of(view.getOrderId())).get(view.getOrderId()),
                SecurityConfig.canWrite());
    }
}
