package xds.lib.easyhttp.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.util.Arrays;

import androidx.annotation.AnyThread;
import androidx.annotation.IntDef;
import androidx.annotation.WorkerThread;

/**
 * Policy of retry request.
 */
public final class RetryPolicy {

    private static final int DEFAULT_COUNT = 3;

    private final int[] reasons;
    private final Delay delay;

    private volatile int count;

    private RetryPolicy(int count, Delay delay, @Reason int... reasons) {
        this.count = count;
        this.reasons = reasons;
        this.delay = delay;
        Arrays.sort(reasons);
    }

    private RetryPolicy(@Reason int... reasons) {
        this(DEFAULT_COUNT, Delay.proportionality, reasons);
    }

    public static RetryPolicy create(int count, Delay time, @Reason int... reasons) {
        return new RetryPolicy(count, time, reasons);
    }

    public static RetryPolicy create(@Reason int... reasons) {
        return new RetryPolicy(reasons);
    }

    public static RetryPolicy create500() {
        return RetryPolicy.create(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                HttpURLConnection.HTTP_NOT_IMPLEMENTED,
                HttpURLConnection.HTTP_BAD_GATEWAY,
                HttpURLConnection.HTTP_UNAVAILABLE,
                HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
                HttpURLConnection.HTTP_VERSION
        );
    }

    @WorkerThread
    public boolean checkNeedToRetry(int reason) {
        if (Arrays.binarySearch(reasons, reason) == -1/*not found*/) {
            return false;
        }
        if (count > 0) {
            prepare();
            return true;
        }
        return false;
    }

    @AnyThread
    public int getCount() {
        return count;
    }

    @WorkerThread
    private void prepare() {
        try {
            Thread.sleep(delay.waiting(count--));
        } catch (InterruptedException e) {
            count = 0;
        }
    }

    public enum Delay {

        proportionality(2L);

        private final long factor;

        Delay(long factor) {
            this.factor = factor;
        }

        private long waiting(int count) {
            return (long) ((float) factor / count + count / 10f) * 1000L;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            /*201+*/
            HttpURLConnection.HTTP_CREATED,
            HttpURLConnection.HTTP_ACCEPTED,
            HttpURLConnection.HTTP_NOT_AUTHORITATIVE,
            HttpURLConnection.HTTP_NO_CONTENT,
            HttpURLConnection.HTTP_RESET,
            HttpURLConnection.HTTP_PARTIAL,
            /*300+*/
            HttpURLConnection.HTTP_MULT_CHOICE,
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            HttpURLConnection.HTTP_NOT_MODIFIED,
            HttpURLConnection.HTTP_USE_PROXY,
            /*500+*/
            HttpURLConnection.HTTP_INTERNAL_ERROR,
            HttpURLConnection.HTTP_NOT_IMPLEMENTED,
            HttpURLConnection.HTTP_BAD_GATEWAY,
            HttpURLConnection.HTTP_UNAVAILABLE,
            HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
            HttpURLConnection.HTTP_VERSION
    })
    public @interface Reason {}
}
