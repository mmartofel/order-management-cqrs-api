package pl.piomin.services.order.command;

import jakarta.validation.Valid;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.piomin.services.api.CommandResult;

import java.util.UUID;

/** Write endpoints of orders; reads are served by the query service on the same path. */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderCommandController {

    private final CommandGateway commandGateway;

    public OrderCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    public ResponseEntity<CommandResult> place(@Valid @RequestBody PlaceOrderRequest request) {
        UUID orderId = UUID.randomUUID();
        commandGateway.sendAndWait(new PlaceOrderCommand(orderId, request.personId(), request.addressId(),
                request.lines().stream().map(l -> new PlaceOrderCommand.Line(l.itemId(), l.quantity())).toList()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(orderId).toUri()).body(new CommandResult(orderId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CommandResult> cancel(@PathVariable UUID id, @Valid @RequestBody CancelOrderRequest request) {
        commandGateway.sendAndWait(new CancelOrderCommand(id, request.reason()));
        return ResponseEntity.accepted().body(new CommandResult(id));
    }
}
