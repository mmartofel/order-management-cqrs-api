package pl.piomin.services.item;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.jpa.domain.Specification;
import pl.piomin.services.api.Like;

import java.util.ArrayList;
import java.util.List;

/** Optional list filters, combined with AND. */
public record ItemFilter(
        @Parameter(description = "Case-insensitive search in SKU and name") String q,
        @Parameter(description = "Only these ids (used by other services to resolve labels)") List<Long> ids) {

    public Specification<Item> toSpecification() {
        List<Specification<Item>> specs = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = Like.contains(q.strip());
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("sku")), pattern, Like.ESCAPE),
                    cb.like(cb.lower(root.get("name")), pattern, Like.ESCAPE)));
        }
        if (ids != null && !ids.isEmpty()) {
            specs.add((root, query, cb) -> root.get("id").in(ids));
        }
        return Specification.allOf(specs);
    }
}
