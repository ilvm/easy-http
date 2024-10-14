package xds.lib.easyhttp.util;

import androidx.annotation.AnyThread;
import androidx.annotation.WorkerThread;

import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import xds.lib.easyhttp.exception.ResponseException;

/**
 * Policy of retry request.
 */
public final class RetryPolicy {

    private final Predicate<Throwable> predicate;
    private final AtomicInteger count;
    private final long delay;

    /**
     * Create instance if {@code RetryPolicy} with the specified params.
     *
     * @param predicate the predicate used to determine whether a retry should be attempted based
     * on the given throwable.
     * @param count the maximum number of retry attempts.
     * @param delay the delay in milliseconds between retry attempts.
     */
    public static RetryPolicy create(Predicate<Throwable> predicate, int count, long delay) {
        return new RetryPolicy(predicate, count, delay);
    }

    /**
     * Create instance if {@code RetryPolicy} with the 500x status code.
     *
     * @param count the maximum number of retry attempts.
     * @param delay the delay in milliseconds between retry attempts.
     */
    public static RetryPolicy create50x(int count, long delay) {
        return new RetryPolicy(throwable -> {
            if (throwable instanceof ResponseException) {
                final int responseCode = ((ResponseException) throwable).getResponseCode();
                switch (responseCode) {
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    case HttpURLConnection.HTTP_NOT_IMPLEMENTED:
                    case HttpURLConnection.HTTP_BAD_GATEWAY:
                    case HttpURLConnection.HTTP_UNAVAILABLE:
                    case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                    case HttpURLConnection.HTTP_VERSION:
                        return true;
                }
            }
            return false;
        }, count, Math.max(0, delay));
    }

    private RetryPolicy(Predicate<Throwable> predicate, int count, long delay) {
        this.predicate = predicate;
        this.count = new AtomicInteger(count);
        this.delay = delay;
    }

    @WorkerThread
    public boolean checkNeedToRetry(Throwable throwable) {
        if (!predicate.test(throwable)) {
            return false;
        }
        if (count.getAndDecrement() > 0) {
            return prepare();
        }
        return false;
    }

    @AnyThread
    public int getCount() {
        return count.get();
    }

    @WorkerThread
    private boolean prepare() {
        try {
            Thread.sleep(delay);
            return true;
        } catch (InterruptedException e) {
            count.set(0);
            return false;
        }
    }
}
