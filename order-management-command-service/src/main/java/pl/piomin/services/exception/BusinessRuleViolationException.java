package pl.piomin.services.exception;

/**
 * A command the aggregate's current state does not allow, for example paying a cancelled order (409, code
 * {@code invalid-state}).
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
