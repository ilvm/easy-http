package xds.lib.easyhttp;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.concurrent.Executor;

import xds.lib.easyhttp.async.ResponseListener;
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
     * @return Parsed response of type {@code T}.
     * @throws RequestException If there is an issue with the request.
     * @throws ResponseException If the server returns an error.
     * @throws ParseException If there is an error parsing the response.
     */
    @WorkerThread
    T execute() throws RequestException, ResponseException, ParseException;

    /**
     * Asynchronously executes the request.
     *
     * @param executor Executor for managing the background task.
     * @param listener Listener to handle the response or any errors.
     */
    @AnyThread
    default void executeAsync(@NonNull Executor executor, @NonNull ResponseListener<T> listener) {
        executeAsync(executor, new Handler(Looper.getMainLooper()), listener);
    }

    /**
     * Asynchronously executes the request.
     *
     * @param executor Executor for managing the background task.
     * @param handler Handler to post the result or error back to the some thread.
     * @param listener Listener to handle the response or any errors.
     * @see java.util.concurrent.Executor
     */
    @AnyThread
    void executeAsync(@NonNull Executor executor, Handler handler,
            @NonNull ResponseListener<T> listener);

    /**
     * The ID identifier of request.
     * Use optional if needed; If not impl will be return {@link Class#getName()}.
     */
    @NonNull
    default String getRequestId() {
        return getClass().getName();
    }
}
