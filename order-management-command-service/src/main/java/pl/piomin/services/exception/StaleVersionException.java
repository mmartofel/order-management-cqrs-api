package pl.piomin.services.exception;

/** The command carried a version that is not the aggregate's current one: the client has to reload first (409). */
public class StaleVersionException extends RuntimeException {

    public StaleVersionException(String message) {
        super(message);
    }
}
