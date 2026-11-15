package pl.piomin.services.address;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.jpa.domain.Specification;
import pl.piomin.services.api.Like;

import java.util.ArrayList;
import java.util.List;

/** Optional list filters, combined with AND. */
public record AddressFilter(
        @Parameter(description = "Case-insensitive search in street, city and postal code") String q,
        @Parameter(description = "Id of the person") Long personId,
        @Parameter(description = "Only these ids (used by other services to resolve labels)") List<Long> ids) {

    public Specification<Address> toSpecification() {
        List<Specification<Address>> specs = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = Like.contains(q.strip());
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("street")), pattern, Like.ESCAPE),
                    cb.like(cb.lower(root.get("city")), pattern, Like.ESCAPE),
                    cb.like(cb.lower(root.get("postalCode")), pattern, Like.ESCAPE)));
        }
        if (personId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("personId"), personId));
        }
        if (ids != null && !ids.isEmpty()) {
            specs.add((root, query, cb) -> root.get("id").in(ids));
        }
        return Specification.allOf(specs);
    }
}
