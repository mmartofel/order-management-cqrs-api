package pl.piomin.services.person;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.jpa.domain.Specification;
import pl.piomin.services.api.Like;

import java.util.ArrayList;
import java.util.List;

/** Optional list filters, combined with AND. */
public record PersonFilter(
        @Parameter(description = "Case-insensitive search in first name, last name and email") String q,
        @Parameter(description = "Only these ids (used by other services to resolve labels)") List<Long> ids) {

    public Specification<Person> toSpecification() {
        List<Specification<Person>> specs = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = Like.contains(q.strip());
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern, Like.ESCAPE),
                    cb.like(cb.lower(root.get("lastName")), pattern, Like.ESCAPE),
                    cb.like(cb.lower(root.get("email")), pattern, Like.ESCAPE)));
        }
        if (ids != null && !ids.isEmpty()) {
            specs.add((root, query, cb) -> root.get("id").in(ids));
        }
        return Specification.allOf(specs);
    }
}
