package pl.piomin.services.order.command;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.piomin.services.api.CommandResult;

import java.util.UUID;

/**
 * Commands of orders. Reads are served by the query service on the same path; the method names are the
 * operationIds of the contract. {@code @ResponseStatus} documents the success status, which springdoc does not
 * take from the returned {@code ResponseEntity}.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
@SecurityRequirement(name = "bearer")
public class OrderCommandController {

    private final CommandGateway commandGateway;

    public OrderCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CommandResult> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        UUID id = UUID.randomUUID();
        long version = commandGateway.sendAndWait(new PlaceOrderCommand(id, request.personId(), request.addressId(),
                request.lines().stream().map(l -> new PlaceOrderCommand.Line(l.itemId(), l.quantity())).toList()),
                Long.class);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(id).toUri()).body(new CommandResult(id, version));
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<CommandResult> cancelOrder(@PathVariable UUID id,
                                                     @Valid @RequestBody CancelOrderRequest request) {
        long version = commandGateway.sendAndWait(new CancelOrderCommand(id, request.reason(), request.version()),
                Long.class);
        return ResponseEntity.accepted().body(new CommandResult(id, version));
    }
}
