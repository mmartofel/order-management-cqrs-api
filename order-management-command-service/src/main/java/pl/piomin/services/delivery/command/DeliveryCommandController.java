package pl.piomin.services.delivery.command;

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
 * Commands of deliveries. Reads are served by the query service on the same path; the method names are the
 * operationIds of the contract. {@code @ResponseStatus} documents the success status, which springdoc does not
 * take from the returned {@code ResponseEntity}.
 */
@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Deliveries")
@SecurityRequirement(name = "bearer")
public class DeliveryCommandController {

    private final CommandGateway commandGateway;

    public DeliveryCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CommandResult> createDelivery(@Valid @RequestBody DeliveryCreateRequest request) {
        UUID id = UUID.randomUUID();
        long version = commandGateway.sendAndWait(new ScheduleDeliveryCommand(id, request.orderId(), request.carrier(),
                request.scheduledFor()), Long.class);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(id).toUri()).body(new CommandResult(id, version));
    }

    @PostMapping("/{id}/ship")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<CommandResult> shipDelivery(@PathVariable UUID id,
                                                      @Valid @RequestBody ShipDeliveryRequest request) {
        long version = commandGateway.sendAndWait(
                new ShipDeliveryCommand(id, request.trackingNumber(), request.version()), Long.class);
        return ResponseEntity.accepted().body(new CommandResult(id, version));
    }

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<CommandResult> completeDelivery(@PathVariable UUID id,
                                                          @Valid @RequestBody CompleteDeliveryRequest request) {
        long version = commandGateway.sendAndWait(new CompleteDeliveryCommand(id, request.version()), Long.class);
        return ResponseEntity.accepted().body(new CommandResult(id, version));
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<CommandResult> cancelDelivery(@PathVariable UUID id,
                                                        @Valid @RequestBody CancelDeliveryRequest request) {
        long version = commandGateway.sendAndWait(new CancelDeliveryCommand(id, request.reason(), request.version()),
                Long.class);
        return ResponseEntity.accepted().body(new CommandResult(id, version));
    }
}
