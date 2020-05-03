package xds.lib.easyhttp.util;

/**
 * Policy of logcat.
 */
public final class LogPolicy {

    private LogPolicy() {}

    /**
     * Show all log level for only debug build variant.
     */
    public static final int ADAPTIVE = 0;

    /**
     * Show warning & error log level for release build variant and all log level for debug build variant.
     */
    public static final int MEDIUM = 1;

    /**
     * Show all log level for both debug and release build variant.
     */
    public static final int AGGRESSIVE = 2;
}
