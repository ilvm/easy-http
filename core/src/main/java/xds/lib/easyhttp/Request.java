package xds.lib.easyhttp;

import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.exception.RequestException;
import xds.lib.easyhttp.exception.ResponseException;

/**
 * The abstract base interface of execute request.
 *
 * @param <T> The type of response expected from the request
 */
public interface Request<T> {

    /**
     * Synchronously executes the request and returns the parsed response.
     *
     * @return parsed response of type {@code T}
     * @throws RequestException if a network or protocol-level error occurs
     * @throws ResponseException if the server responds with a non-success HTTP status code
     * @throws ParseException if the response cannot be parsed
     */
    T execute() throws RequestException, ResponseException, ParseException;

    /**
     * Returns an optional identifier for this request.
     * <p>
     * Can be used for logging, tracking, or debugging purposes.
     * Defaults to the fully qualified class name.
     */
    default String getRequestId() {
        return getClass().getName();
    }
}
