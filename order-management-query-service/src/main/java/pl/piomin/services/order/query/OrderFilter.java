package pl.piomin.services.order.query;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.jpa.domain.Specification;
import pl.piomin.services.api.Like;
import pl.piomin.services.order.domain.OrderStatus;

import java.util.ArrayList;
import java.util.List;

/** Optional list filters, combined with AND. */
public record OrderFilter(
        @Parameter(description = "Case-insensitive search in item names and SKUs") String q,
        @Parameter(description = "Id of the person who placed the order") Long personId,
        @Parameter(description = "Exact status") OrderStatus status) {

    public Specification<OrderView> toSpecification() {
        List<Specification<OrderView>> specs = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = Like.contains(q.strip());
            specs.add((root, query, cb) -> {
                var sub = query.subquery(Integer.class);
                var order = sub.correlate(root);
                var line = order.join("lines");
                sub.select(cb.literal(1)).where(cb.or(
                        cb.like(cb.lower(line.get("name")), pattern, Like.ESCAPE),
                        cb.like(cb.lower(line.get("sku")), pattern, Like.ESCAPE)));
                return cb.exists(sub);
            });
        }
        if (personId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("personId"), personId));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return Specification.allOf(specs);
    }
}
