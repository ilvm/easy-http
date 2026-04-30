package xds.lib.easyhttp.exception;

import java.io.IOException;

/** Thrown when an error occurs during request execution. */
public class RequestException extends IOException {

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
