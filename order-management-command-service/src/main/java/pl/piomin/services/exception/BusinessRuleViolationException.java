package pl.piomin.services.exception;

/** A command that breaks a rule of the aggregate's current state, for example paying a cancelled order (409). */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
