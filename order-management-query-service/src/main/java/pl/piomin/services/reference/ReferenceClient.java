package pl.piomin.services.reference;

import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Collection;

/** Declarative client of the reference service; only the fields the labels need are mapped. */
@HttpExchange("/api")
public interface ReferenceClient {

    record PersonRef(Long id, String firstName, String lastName) {
    }

    record AddressRef(Long id, String street, String postalCode, String city) {
    }

    @GetExchange("/persons")
    PagedModel<PersonRef> persons(@RequestParam Collection<Long> ids, @RequestParam int size);

    @GetExchange("/addresses")
    PagedModel<AddressRef> addresses(@RequestParam Collection<Long> ids, @RequestParam int size);
}
