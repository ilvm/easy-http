package xds.lib.easyhttp.exception;

/**
 * The exception of parsing failed.
 */
public final class ParseException extends Exception {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
