package xds.lib.easyhttp.async;

import androidx.annotation.NonNull;

/**
 * The callback from {@link xds.lib.easyhttp.Request}.
 *
 * @param <T> The class of response.
 */
public interface ResponseListener<T> {

    /**
     * Request was success.
     *
     * @param response Result of request.
     * @param requestId The ID identifier of request.
     */
    void onSuccess(@NonNull T response, @NonNull String requestId);

    /**
     * Request was failed.
     *
     * @param exception Error of request.
     * @param requestId The ID identifier of request.
     */
    void onFailed(@NonNull Throwable exception, @NonNull String requestId);
}
