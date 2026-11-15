package pl.piomin.services.payment.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read endpoints of payments; eventually consistent with the command service. */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentQueryController {

    private final FindPaymentHandler findPayment;
    private final ListPaymentsHandler listPayments;

    public PaymentQueryController(FindPaymentHandler findPayment, ListPaymentsHandler listPayments) {
        this.findPayment = findPayment;
        this.listPayments = listPayments;
    }

    @GetMapping
    public PagedModel<PaymentResponse> list(@RequestParam(required = false) UUID orderId, Pageable pageable) {
        return new PagedModel<>(listPayments.handle(new ListPaymentsQuery(orderId, pageable)));
    }

    @GetMapping("/{id}")
    public PaymentResponse find(@PathVariable UUID id) {
        return findPayment.handle(new FindPaymentQuery(id));
    }
}
