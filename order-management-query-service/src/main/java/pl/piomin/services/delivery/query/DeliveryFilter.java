package pl.piomin.services.delivery.query;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.jpa.domain.Specification;
import pl.piomin.services.api.Like;
import pl.piomin.services.delivery.domain.DeliveryStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Optional list filters, combined with AND. */
public record DeliveryFilter(
        @Parameter(description = "Case-insensitive search in the carrier and tracking number") String q,
        @Parameter(description = "Id of the order") UUID orderId,
        @Parameter(description = "Exact status") DeliveryStatus status) {

    public Specification<DeliveryView> toSpecification() {
        List<Specification<DeliveryView>> specs = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = Like.contains(q.strip());
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("carrier")), pattern, Like.ESCAPE),
                    cb.like(cb.lower(root.get("trackingNumber")), pattern, Like.ESCAPE)));
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
