package pl.piomin.services.payment.query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Transactional(readOnly = true)
public class FindPaymentHandler {

    private final PaymentViewRepository repository;

    public FindPaymentHandler(PaymentViewRepository repository) {
        this.repository = repository;
    }

    public PaymentResponse handle(FindPaymentQuery query) {
        return repository.findById(query.paymentId()).map(PaymentResponse::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment " + query.paymentId() + " not found"));
    }
}
