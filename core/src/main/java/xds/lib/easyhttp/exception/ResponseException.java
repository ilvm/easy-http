package xds.lib.easyhttp.exception;

/**
 * The exception if the server answer with error code.
 */
public final class ResponseException extends Exception {

    private final int responseCode;

    public ResponseException(String message, int responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
