package xds.lib.easyhttp;

import android.os.Handler;

import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import xds.lib.easyhttp.async.ResponseListener;
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
     * Execute request synchronous.
     *
     * @return Response data by typed class or throw if some problems.
     * @throws RequestException  If the request failed.
     * @throws ResponseException If the server answer with error code.
     * @throws ParseException    If the parse failed.
     */
    @NonNull
    @WorkerThread
    T execute() throws RequestException, ResponseException, ParseException;

    /**
     * Execute request asynchronous.
     *
     * @param executor Executor for run task in background {@link java.util.concurrent.Executor}.
     * @param listener Callback result.
     */
    @AnyThread
    void executeAsync(@NonNull Executor executor, @NonNull ResponseListener<T> listener);

    /**
     * Execute request asynchronous.
     *
     * @param executor Executor for run task in background {@link java.util.concurrent.Executor}.
     * @param handler  Handler for call callback on needed thread.
     * @param listener Callback result.
     */
    @AnyThread
    void executeAsync(@NonNull Executor executor, @Nullable Handler handler, @NonNull ResponseListener<T> listener);

    /**
     * The ID identifier of request.
     * Use optional if needed; If not impl will be return {@link Class#getName()}.
     */
    @NonNull
    default String getRequestId() {
        return getClass().getName();
    }
}
