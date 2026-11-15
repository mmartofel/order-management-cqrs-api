package pl.piomin.services.delivery.command;

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

/** Write endpoints of deliveries; reads are served by the query service on the same path. */
@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryCommandController {

    private final CommandGateway commandGateway;

    public DeliveryCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    public ResponseEntity<CommandResult> schedule(@Valid @RequestBody ScheduleDeliveryRequest request) {
        UUID deliveryId = UUID.randomUUID();
        commandGateway.sendAndWait(new ScheduleDeliveryCommand(deliveryId, request.orderId(), request.carrier(),
                request.scheduledFor()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(deliveryId).toUri()).body(new CommandResult(deliveryId));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<CommandResult> ship(@PathVariable UUID id, @Valid @RequestBody ShipDeliveryRequest request) {
        commandGateway.sendAndWait(new ShipDeliveryCommand(id, request.trackingNumber()));
        return ResponseEntity.accepted().body(new CommandResult(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CommandResult> complete(@PathVariable UUID id) {
        commandGateway.sendAndWait(new CompleteDeliveryCommand(id));
        return ResponseEntity.accepted().body(new CommandResult(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CommandResult> cancel(@PathVariable UUID id,
                                                @Valid @RequestBody CancelDeliveryRequest request) {
        commandGateway.sendAndWait(new CancelDeliveryCommand(id, request.reason()));
        return ResponseEntity.accepted().body(new CommandResult(id));
    }
}
