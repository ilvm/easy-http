package xds.lib.easyhttp;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.exception.RequestException;
import xds.lib.easyhttp.exception.ResponseException;

/**
 * The abstract base interface of execute request.
 *
 * @param <T> The class of response.
 */
public interface Request<T> {

    /**
     * Execute request.
     *
     * @return Response data by typed class or throw if some problems.
     * @throws RequestException  If the request failed.
     * @throws ResponseException If the server answer with error code.
     * @throws ParseException    If the parse failed.
     */
    @NonNull
    @WorkerThread
    T execute() throws RequestException, ResponseException, ParseException;
}
