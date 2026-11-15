package pl.piomin.services.delivery.query;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read endpoints of deliveries; eventually consistent with the command service, which serves the writes on the same
 * path. The method names are the operationIds of the contract.
 */
@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Deliveries")
@SecurityRequirement(name = "bearer")
public class DeliveryQueryController {

    private final FindDeliveryHandler findDelivery;
    private final ListDeliveriesHandler listDeliveries;

    public DeliveryQueryController(FindDeliveryHandler findDelivery, ListDeliveriesHandler listDeliveries) {
        this.findDelivery = findDelivery;
        this.listDeliveries = listDeliveries;
    }

    @GetMapping
    public PagedModel<DeliveryResponse> listDeliveries(@ParameterObject @Valid DeliveryFilter filter,
                                                       @ParameterObject Pageable pageable) {
        return new PagedModel<>(listDeliveries.handle(filter, pageable));
    }

    @GetMapping("/{id}")
    public DeliveryResponse getDelivery(@PathVariable UUID id) {
        return findDelivery.handle(new FindDeliveryQuery(id));
    }
}
