package xds.lib.easyhttp.exception;

/** Thrown when the server responds with a non-success HTTP status code. */
public class ResponseException extends Exception {

    private final int responseCode;

    public ResponseException(String message, int responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public ResponseException(String message, int responseCode, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    public ResponseException(int responseCode) {
        super("HTTP error: " + responseCode);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
