package pl.piomin.services.reference;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.math.BigDecimal;

/** Declarative client of the reference service; only the fields the command side needs are mapped. */
@HttpExchange("/api")
public interface ReferenceClient {

    record PersonRef(Long id) {
    }

    record AddressRef(Long id, Long personId) {
    }

    record ItemRef(Long id, String sku, String name, BigDecimal price) {
    }

    @GetExchange("/persons/{id}")
    PersonRef person(@PathVariable Long id);

    @GetExchange("/addresses/{id}")
    AddressRef address(@PathVariable Long id);

    @GetExchange("/items/{id}")
    ItemRef item(@PathVariable Long id);
}
