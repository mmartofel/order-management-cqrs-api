package pl.piomin.services.reference;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Collection;
import java.util.List;

/** Declarative client of the reference service; only the fields the labels need are mapped. */
@HttpExchange("/api")
public interface ReferenceClient {

    record PersonRef(Long id, String firstName, String lastName) {
    }

    record AddressRef(Long id, String street, String postalCode, String city) {
    }

    /** The {@code content} of the reference service's {@code PagedModel}; its {@code page} metadata is not needed. */
    record Page<T>(List<T> content) {
    }

    @GetExchange("/persons")
    Page<PersonRef> persons(@RequestParam Collection<Long> ids, @RequestParam int size);

    @GetExchange("/addresses")
    Page<AddressRef> addresses(@RequestParam Collection<Long> ids, @RequestParam int size);
}
