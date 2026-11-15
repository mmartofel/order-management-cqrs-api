package pl.piomin.services.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import pl.piomin.services.api.Reference;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Display names of people and addresses, read from the reference service when a response is built: one call per
 * relation and page. Events keep only ids, so names live only in the reference service and erasing a person there
 * erases them here too. An id the reference service does not know (erased) or cannot answer for (down, or an answer
 * this client cannot read) gets a generic label such as "Person 7", so reads keep working.
 */
@Component
public class ReferenceLabels {

    private static final Logger log = LoggerFactory.getLogger(ReferenceLabels.class);

    /** The reference service's maximum page size. */
    private static final int MAX_IDS = 100;

    private final ReferenceClient client;

    public ReferenceLabels(ReferenceClient client) {
        this.client = client;
    }

    public Function<Long, Reference> persons(Collection<Long> ids) {
        Map<Long, String> labels = fetch(ids, "persons", batch -> client.persons(batch, MAX_IDS).content().stream()
                .collect(Collectors.toMap(ReferenceClient.PersonRef::id, p -> p.firstName() + " " + p.lastName())));
        return id -> new Reference(id, labels.getOrDefault(id, "Person " + id));
    }

    public Function<Long, Reference> addresses(Collection<Long> ids) {
        Map<Long, String> labels = fetch(ids, "addresses", batch -> client.addresses(batch, MAX_IDS).content()
                .stream().collect(Collectors.toMap(ReferenceClient.AddressRef::id,
                        a -> a.street() + ", " + a.postalCode() + " " + a.city())));
        return id -> new Reference(id, labels.getOrDefault(id, "Address " + id));
    }

    private Map<Long, String> fetch(Collection<Long> ids, String relation,
                                    Function<List<Long>, Map<Long, String>> call) {
        Set<Long> distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (distinct.isEmpty()) {
            return Map.of();
        }
        try {
            return call.apply(distinct.stream().limit(MAX_IDS).toList());
        } catch (RestClientException | HttpMessageConversionException e) {
            log.atWarn().addKeyValue("relation", relation).setCause(e)
                    .log("Reference service did not answer; generic labels used");
            return Map.of();
        }
    }
}
