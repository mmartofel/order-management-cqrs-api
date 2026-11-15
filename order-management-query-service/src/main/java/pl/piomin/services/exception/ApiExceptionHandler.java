package pl.piomin.services.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** All errors are RFC 9457 Problem Details. An invalid filter value is a 400 with a field error named after it. */
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

    /** 400 with one field error per invalid filter, such as {@code status=UNKNOWN} or a malformed {@code orderId}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ex.getBody().setProperty("errors", ex.getFieldErrors().stream()
                .map(e -> new FieldMessage(e.getField(), INVALID, "has an invalid value"))
                .toList());
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
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
