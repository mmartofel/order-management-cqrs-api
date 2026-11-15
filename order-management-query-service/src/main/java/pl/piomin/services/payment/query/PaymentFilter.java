package pl.piomin.services.payment.query;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.jpa.domain.Specification;
import pl.piomin.services.api.Like;
import pl.piomin.services.payment.domain.PaymentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Optional list filters, combined with AND. */
public record PaymentFilter(
        @Parameter(description = "Case-insensitive search in the transaction reference") String q,
        @Parameter(description = "Id of the order") UUID orderId,
        @Parameter(description = "Exact status") PaymentStatus status) {

    public Specification<PaymentView> toSpecification() {
        List<Specification<PaymentView>> specs = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = Like.contains(q.strip());
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("transactionReference")), pattern, Like.ESCAPE));
        }
        if (orderId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("orderId"), orderId));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return Specification.allOf(specs);
    }
}
