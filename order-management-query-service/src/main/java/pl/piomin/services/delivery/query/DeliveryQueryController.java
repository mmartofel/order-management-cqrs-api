package pl.piomin.services.delivery.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read endpoints of deliveries; eventually consistent with the command service. */
@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryQueryController {

    private final FindDeliveryHandler findDelivery;
    private final ListDeliveriesHandler listDeliveries;

    public DeliveryQueryController(FindDeliveryHandler findDelivery, ListDeliveriesHandler listDeliveries) {
        this.findDelivery = findDelivery;
        this.listDeliveries = listDeliveries;
    }

    @GetMapping
    public PagedModel<DeliveryResponse> list(@RequestParam(required = false) UUID orderId, Pageable pageable) {
        return new PagedModel<>(listDeliveries.handle(new ListDeliveriesQuery(orderId, pageable)));
    }

    @GetMapping("/{id}")
    public DeliveryResponse find(@PathVariable UUID id) {
        return findDelivery.handle(new FindDeliveryQuery(id));
    }
}
