package xds.lib.easyhttp;

import android.util.Log;

import xds.lib.easyhttp.util.LogPolicy;

final class Logcat {

    private static final boolean DEBUG = BuildConfig.DEBUG;
    private final int policy;

    Logcat(int policy) {
        this.policy = policy;
    }

    void d(String tag, String msg) {
        if (policy == LogPolicy.AGGRESSIVE || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.d(tag, msg);
        }
    }

    void d(String tag, String format, Object... args) {
        if (policy == LogPolicy.AGGRESSIVE || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.d(tag, String.format(format, args));
        }
    }

    void i(String tag, String msg) {
        if (policy == LogPolicy.AGGRESSIVE || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.i(tag, msg);
        }
    }

    void i(String tag, String format, Object... args) {
        if (policy == LogPolicy.AGGRESSIVE || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.i(tag, String.format(format, args));
        }
    }

    void w(String tag, String msg) {
        if (policy == LogPolicy.AGGRESSIVE || policy == LogPolicy.MEDIUM || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.w(tag, msg);
        }
    }

    void w(String tag, String msg, Throwable throwable) {
        if (policy == LogPolicy.AGGRESSIVE || policy == LogPolicy.MEDIUM || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.w(tag, msg, throwable);
        }
    }

    void w(String tag, String format, Object... args) {
        if (policy == LogPolicy.AGGRESSIVE || policy == LogPolicy.MEDIUM || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.w(tag, String.format(format, args));
        }
    }

    void e(String tag, String msg) {
        if (policy == LogPolicy.AGGRESSIVE || policy == LogPolicy.MEDIUM || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.e(tag, msg);
        }
    }

    void e(String tag, String msg, Throwable throwable) {
        if (policy == LogPolicy.AGGRESSIVE || policy == LogPolicy.MEDIUM || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.e(tag, msg, throwable);
        }
    }

    void e(String tag, String format, Object... args) {
        if (policy == LogPolicy.AGGRESSIVE || policy == LogPolicy.MEDIUM || (policy == LogPolicy.ADAPTIVE && DEBUG)) {
            Log.e(tag, String.format(format, args));
        }
    }
}
