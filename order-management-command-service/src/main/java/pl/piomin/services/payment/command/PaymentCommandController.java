package pl.piomin.services.payment.command;

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
 * Commands of payments. Reads are served by the query service on the same path; the method names are the
 * operationIds of the contract. {@code @ResponseStatus} documents the success status, which springdoc does not
 * take from the returned {@code ResponseEntity}.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments")
@SecurityRequirement(name = "bearer")
public class PaymentCommandController {

    private final CommandGateway commandGateway;

    public PaymentCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CommandResult> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        UUID id = UUID.randomUUID();
        long version = commandGateway.sendAndWait(new InitiatePaymentCommand(id, request.orderId(), request.amount(),
                request.method().name()), Long.class);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(id).toUri()).body(new CommandResult(id, version));
    }

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<CommandResult> completePayment(@PathVariable UUID id,
                                                         @Valid @RequestBody CompletePaymentRequest request) {
        long version = commandGateway.sendAndWait(new CompletePaymentCommand(id, request.transactionReference(),
                request.version()), Long.class);
        return ResponseEntity.accepted().body(new CommandResult(id, version));
    }

    @PostMapping("/{id}/fail")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<CommandResult> failPayment(@PathVariable UUID id,
                                                     @Valid @RequestBody FailPaymentRequest request) {
        long version = commandGateway.sendAndWait(new FailPaymentCommand(id, request.reason(), request.version()),
                Long.class);
        return ResponseEntity.accepted().body(new CommandResult(id, version));
    }

    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<CommandResult> refundPayment(@PathVariable UUID id,
                                                       @Valid @RequestBody RefundPaymentRequest request) {
        long version = commandGateway.sendAndWait(new RefundPaymentCommand(id, request.reason(), request.version()),
                Long.class);
        return ResponseEntity.accepted().body(new CommandResult(id, version));
    }
}
