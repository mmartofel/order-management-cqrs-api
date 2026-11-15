package pl.piomin.services.order.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read endpoints of orders; eventually consistent with the command service. */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderQueryController {

    private final FindOrderHandler findOrder;
    private final ListOrdersHandler listOrders;

    public OrderQueryController(FindOrderHandler findOrder, ListOrdersHandler listOrders) {
        this.findOrder = findOrder;
        this.listOrders = listOrders;
    }

    @GetMapping
    public PagedModel<OrderSummaryResponse> list(@RequestParam(required = false) Long personId,
                                                 @RequestParam(required = false) OrderStatus status,
                                                 Pageable pageable) {
        return new PagedModel<>(listOrders.handle(new ListOrdersQuery(personId, status, pageable)));
    }

    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable UUID id) {
        return findOrder.handle(new FindOrderQuery(id));
    }
}
