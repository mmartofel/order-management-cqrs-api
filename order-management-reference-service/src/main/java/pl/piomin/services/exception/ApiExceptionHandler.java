package pl.piomin.services.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.piomin.services.item.Item;
import pl.piomin.services.person.Person;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** All errors are RFC 9457 Problem Details; field-level errors are listed in {@code errors}. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String REQUIRED = "required";
    public static final String EMAIL = "email";
    public static final String UNIQUE = "unique";
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
            case "NotBlank", "NotNull" -> REQUIRED;
            case "Email" -> EMAIL;
            case null, default -> INVALID;
        };
    }

    /** 409: a unique value already in use, or a record that other records still reference. */
    @ExceptionHandler
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        String constraint = ex.getCause() instanceof ConstraintViolationException cve ? cve.getConstraintName() : null;
        if (Person.UNIQUE_EMAIL.equals(constraint)) {
            problem.setDetail("Email is already in use");
            problem.setProperty("errors", List.of(new FieldMessage("email", UNIQUE, "already in use")));
        } else if (Item.UNIQUE_SKU.equals(constraint)) {
            problem.setDetail("SKU is already in use");
            problem.setProperty("errors", List.of(new FieldMessage("sku", UNIQUE, "already in use")));
        } else if (constraint != null && constraint.startsWith("fk_")) {
            problem.setDetail("Record is still referenced by other records");
        } else {
            problem.setDetail("Request conflicts with existing data");
        }
        return problem;
    }

    /** 409 without {@code errors}: the client sent a stale version and has to reload. */
    @ExceptionHandler
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Record was changed by someone else; reload and retry");
    }

    /** 400 for {@code ?sort=unknownProperty}. */
    @ExceptionHandler
    ProblemDetail handleBadSort(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 500 for anything not handled above; the cause is logged, never returned to the client. */
    @ExceptionHandler
    ProblemDetail handleUnexpected(Exception ex) {
        logger.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
