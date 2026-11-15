package pl.piomin.services.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException;
import org.axonframework.modelling.repository.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * All errors are RFC 9457 Problem Details; field-level errors are listed in {@code errors}. The UI tells the two 409
 * cases apart by {@code errors}: without it the version was stale, with {@code invalid-state} the state forbids the
 * command.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String REQUIRED_CODE = "required";
    public static final String EMAIL = "email";
    public static final String UNIQUE = "unique";
    public static final String INVALID = "invalid";
    public static final String NOT_FOUND = "not-found";
    public static final String IN_USE = "in-use";
    public static final String INVALID_STATE = "invalid-state";

    /**
     * Field-level error; {@code code} is stable for UI translation, {@code message} is an English fallback.
     * {@code field} is absent for errors about the whole record ({@code in-use}, {@code invalid-state}). The code list
     * is the same in every service of this API, so the merged contract has one {@code FieldError}.
     */
    @Schema(name = "FieldError")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldMessage(String field,
                               @Schema(requiredMode = REQUIRED, allowableValues = {REQUIRED_CODE, EMAIL, UNIQUE,
                                       INVALID, NOT_FOUND, IN_USE, INVALID_STATE}) String code,
                               @Schema(requiredMode = REQUIRED) String message) {
    }

    /** RFC 9457 Problem Details as returned by this API; used only to document error responses. */
    @Schema(name = "Problem")
    public record Problem(URI type,
                          @Schema(requiredMode = REQUIRED) String title,
                          @Schema(requiredMode = REQUIRED) int status,
                          String detail, URI instance, List<FieldMessage> errors) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<FieldMessage> errors = ex.getFieldErrors().stream()
                .map(e -> new FieldMessage(e.getField(), code(e.getCode()), e.getDefaultMessage()))
                .toList();
        // An empty required text breaks both @NotBlank and @Size(min = 1); report only "required" for that field.
        Set<String> missing = errors.stream().filter(e -> REQUIRED_CODE.equals(e.code())).map(FieldMessage::field)
                .collect(Collectors.toSet());
        ex.getBody().setProperty("errors", errors.stream()
                .filter(e -> REQUIRED_CODE.equals(e.code()) || !missing.contains(e.field()))
                .toList());
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    /**
     * 400 with {@code invalid} on the field when a value cannot be read, for example an unknown enum constant; the
     * rest of an unreadable body is a plain 400.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is not valid");
        if (ex.getCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
            withErrors(problem, new FieldMessage(path(jackson), INVALID, "has an invalid value"));
        }
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** {@code lines[0].itemId} for a value inside a list. */
    private static String path(JacksonException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : ex.getPath()) {
            if (reference.getPropertyName() != null) {
                path.append(path.isEmpty() ? "" : ".").append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String code(String constraint) {
        return switch (constraint) {
            case "NotBlank", "NotNull", "NotEmpty" -> REQUIRED_CODE;
            case null, default -> INVALID;
        };
    }

    /** 404: the command targets an order, payment or delivery that does not exist. */
    @ExceptionHandler
    ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found");
    }

    /** 409 with {@code invalid-state}: the command breaks a rule of the current state (shipping an unpaid order). */
    @ExceptionHandler
    ProblemDetail handleRuleViolation(BusinessRuleViolationException ex) {
        return withErrors(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()),
                new FieldMessage(null, INVALID_STATE, ex.getMessage()));
    }

    /** 409 without {@code errors}: the client's version is stale; it reloads and decides again. */
    @ExceptionHandler
    ProblemDetail handleStaleVersion(StaleVersionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Resource was changed by someone else; reload and retry");
    }

    /** 409 without {@code errors}: another command changed the same aggregate between load and append. */
    @ExceptionHandler
    ProblemDetail handleConcurrentChange(AppendEventsTransactionRejectedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Resource was changed by someone else; reload and retry");
    }

    /** 400 with {@code not-found} on the field that references a person, address, item or order that does not exist. */
    @ExceptionHandler
    ProblemDetail handleInvalidReference(InvalidReferenceException ex) {
        return withErrors(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage()),
                new FieldMessage(ex.field(), NOT_FOUND, "does not exist"));
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

    private static ProblemDetail withErrors(ProblemDetail problem, FieldMessage error) {
        problem.setProperty("errors", List.of(error));
        return problem;
    }
}
