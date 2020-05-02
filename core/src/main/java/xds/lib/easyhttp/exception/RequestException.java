package xds.lib.easyhttp.exception;

/**
 * The exception of request failed.
 */
public final class RequestException extends Exception {

    public RequestException(String message) {
        super(message);
    }

    public RequestException(Throwable cause) {
        super(cause);
    }

    public RequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
