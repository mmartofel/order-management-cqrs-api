package pl.piomin.services.exception;

/** A command that references a person, address or item that does not exist in the reference service (422). */
public class InvalidReferenceException extends RuntimeException {

    public InvalidReferenceException(String message) {
        super(message);
    }
}
