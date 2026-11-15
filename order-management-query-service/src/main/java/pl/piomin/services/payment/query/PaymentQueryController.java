package pl.piomin.services.payment.query;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read endpoints of payments; eventually consistent with the command service, which serves the writes on the same
 * path. The method names are the operationIds of the contract.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments")
@SecurityRequirement(name = "bearer")
public class PaymentQueryController {

    private final FindPaymentHandler findPayment;
    private final ListPaymentsHandler listPayments;

    public PaymentQueryController(FindPaymentHandler findPayment, ListPaymentsHandler listPayments) {
        this.findPayment = findPayment;
        this.listPayments = listPayments;
    }

    @GetMapping
    public PagedModel<PaymentResponse> listPayments(@ParameterObject @Valid PaymentFilter filter,
                                                    @ParameterObject Pageable pageable) {
        return new PagedModel<>(listPayments.handle(filter, pageable));
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return findPayment.handle(new FindPaymentQuery(id));
    }
}
