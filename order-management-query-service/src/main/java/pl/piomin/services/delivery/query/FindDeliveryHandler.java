package pl.piomin.services.delivery.query;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@Transactional(readOnly = true)
public class FindDeliveryHandler {

    private final DeliveryViewRepository repository;

    public FindDeliveryHandler(DeliveryViewRepository repository) {
        this.repository = repository;
    }

    public DeliveryResponse handle(FindDeliveryQuery query) {
        return repository.findById(query.deliveryId()).map(DeliveryResponse::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Delivery " + query.deliveryId() + " not found"));
    }
}
