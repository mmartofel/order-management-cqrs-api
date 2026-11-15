package pl.piomin.services.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.piomin.services.item.Item;
import pl.piomin.services.person.Person;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** All errors are RFC 9457 Problem Details; field-level errors are listed in {@code errors}. */
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

    /** 400: {@code field} references a record that does not exist. */
    public static ErrorResponseException notFound(String field) {
        return problem(HttpStatus.BAD_REQUEST, "Referenced record does not exist",
                new FieldMessage(field, NOT_FOUND, "does not exist"));
    }

    /** 409: the record is still referenced by other records and cannot be deleted. */
    public static ErrorResponseException inUse(String detail, String message) {
        return problem(HttpStatus.CONFLICT, detail, new FieldMessage(null, IN_USE, message));
    }

    private static ErrorResponseException problem(HttpStatus status, String detail, FieldMessage error) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errors", List.of(error));
        return new ErrorResponseException(status, problem, null);
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

    private static String code(String constraint) {
        return switch (constraint) {
            case "NotBlank", "NotNull" -> REQUIRED_CODE;
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
            // a backstop: services check references first and answer 409 in-use themselves
            problem.setDetail("Record is still referenced by other records");
            problem.setProperty("errors", List.of(new FieldMessage(null, IN_USE, "still referenced")));
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
