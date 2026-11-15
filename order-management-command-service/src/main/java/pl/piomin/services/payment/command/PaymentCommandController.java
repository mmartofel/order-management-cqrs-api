package pl.piomin.services.payment.command;

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

/** Write endpoints of payments; reads are served by the query service on the same path. */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentCommandController {

    private final CommandGateway commandGateway;

    public PaymentCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    public ResponseEntity<CommandResult> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        UUID paymentId = UUID.randomUUID();
        commandGateway.sendAndWait(new InitiatePaymentCommand(paymentId, request.orderId(), request.amount(),
                request.method()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(paymentId).toUri()).body(new CommandResult(paymentId));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CommandResult> complete(@PathVariable UUID id,
                                                  @Valid @RequestBody CompletePaymentRequest request) {
        commandGateway.sendAndWait(new CompletePaymentCommand(id, request.transactionReference()));
        return ResponseEntity.accepted().body(new CommandResult(id));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<CommandResult> fail(@PathVariable UUID id, @Valid @RequestBody PaymentReasonRequest request) {
        commandGateway.sendAndWait(new FailPaymentCommand(id, request.reason()));
        return ResponseEntity.accepted().body(new CommandResult(id));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<CommandResult> refund(@PathVariable UUID id,
                                                @Valid @RequestBody PaymentReasonRequest request) {
        commandGateway.sendAndWait(new RefundPaymentCommand(id, request.reason()));
        return ResponseEntity.accepted().body(new CommandResult(id));
    }
}
