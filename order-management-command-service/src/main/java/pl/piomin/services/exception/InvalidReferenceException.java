package pl.piomin.services.exception;

/**
 * A command that references a person, address, item or order that does not exist (400, code {@code not-found} on
 * {@link #field()}).
 */
public class InvalidReferenceException extends RuntimeException {

    private final String field;

    public InvalidReferenceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
