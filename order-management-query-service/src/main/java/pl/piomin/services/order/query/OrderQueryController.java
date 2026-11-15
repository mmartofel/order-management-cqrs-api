package pl.piomin.services.order.query;

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
 * Read endpoints of orders; eventually consistent with the command service, which serves the writes on the same
 * path. The method names are the operationIds of the contract.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
@SecurityRequirement(name = "bearer")
public class OrderQueryController {

    private final FindOrderHandler findOrder;
    private final ListOrdersHandler listOrders;

    public OrderQueryController(FindOrderHandler findOrder, ListOrdersHandler listOrders) {
        this.findOrder = findOrder;
        this.listOrders = listOrders;
    }

    @GetMapping
    public PagedModel<OrderSummaryResponse> listOrders(@ParameterObject @Valid OrderFilter filter,
                                                       @ParameterObject Pageable pageable) {
        return new PagedModel<>(listOrders.handle(filter, pageable));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return findOrder.handle(new FindOrderQuery(id));
    }
}
