package pl.piomin.services.exception;

import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException;
import org.axonframework.modelling.repository.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** All errors are RFC 9457 Problem Details; field-level errors are listed in {@code errors}. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String REQUIRED = "required";
    public static final String INVALID = "invalid";

    /** {@code code} is stable for clients; {@code message} is an English fallback. */
    public record FieldMessage(String field, String code, String message) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<FieldMessage> errors = ex.getFieldErrors().stream()
                .map(e -> new FieldMessage(e.getField(), code(e.getCode()), e.getDefaultMessage()))
                .toList();
        // An empty required text breaks both @NotBlank and @Size(min = 1); report only "required" for that field.
        Set<String> missing = errors.stream().filter(e -> REQUIRED.equals(e.code())).map(FieldMessage::field)
                .collect(Collectors.toSet());
        ex.getBody().setProperty("errors", errors.stream()
                .filter(e -> REQUIRED.equals(e.code()) || !missing.contains(e.field()))
                .toList());
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    private static String code(String constraint) {
        return switch (constraint) {
            case "NotBlank", "NotNull", "NotEmpty" -> REQUIRED;
            case null, default -> INVALID;
        };
    }

    /** 404: the command targets an order, payment or delivery that does not exist. */
    @ExceptionHandler
    ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found");
    }

    /** 409: the command breaks a rule of the current state, for example shipping an unpaid order. */
    @ExceptionHandler
    ProblemDetail handleRuleViolation(BusinessRuleViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** 409: another command changed the same aggregate between load and append; the client can retry. */
    @ExceptionHandler
    ProblemDetail handleConcurrentChange(AppendEventsTransactionRejectedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Resource was changed by another request; retry the command");
    }

    /** 422: the command references a person, address, item or order that does not exist. */
    @ExceptionHandler
    ProblemDetail handleInvalidReference(InvalidReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    /** 503: the reference service cannot be reached; nothing was written. */
    @ExceptionHandler
    ProblemDetail handleReferenceServiceDown(ResourceAccessException ex) {
        logger.warn("Reference service unavailable", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "A dependent service is unavailable; retry later");
    }

    /** 500 for anything not handled above; the cause is logged, never returned to the client. */
    @ExceptionHandler
    ProblemDetail handleUnexpected(Exception ex) {
        logger.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
