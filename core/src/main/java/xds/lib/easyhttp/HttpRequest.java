package xds.lib.easyhttp;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import xds.lib.easyhttp.async.ResponseListener;
import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.exception.RequestException;
import xds.lib.easyhttp.exception.ResponseException;
import xds.lib.easyhttp.util.IOUtils;
import xds.lib.easyhttp.util.LogPolicy;
import xds.lib.easyhttp.util.RetryPolicy;

/**
 * The wrapper of http client for work with REST API.
 *
 * @see HttpURLConnection
 */
public abstract class HttpRequest<T> implements Request<T> {

    private static final String TAG = "HttpRequest";

    private static final int USE_DEFAULT_TIMEOUT = -1;

    private static final String ENCODING_GZIP = "gzip";
    private static final String ENCODING_DEFLATE = "deflate";

    /**
     * The data sent to the server with stored in the query string (name/value) of the HTTP request.
     */
    protected static final String REQUEST_TYPE_GET = "GET";

    /**
     * The data sent to the server with POST is stored in the request body of the HTTP request.
     */
    protected static final String REQUEST_TYPE_POST = "POST";

    /**
     * The data sent to the server with PUT is stored in the request body of the HTTP request.
     */
    protected static final String REQUEST_TYPE_PUT = "PUT";

    protected static final String SCHEME_HTTP = "http";
    protected static final String SCHEME_HTTPS = "https";

    private final RetryPolicy retryPolicy;
    private final Logcat logcat;

    protected HttpRequest() {
        retryPolicy = getRetryPolicy();
        logcat = new Logcat(getLogPolicy());
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @WorkerThread
    @Override
    public T execute() throws RequestException, ResponseException, ParseException {
        synchronized (this) {
            return doRequest();
        }
    }

    @MainThread
    @Override
    public void executeAsync(@NonNull Executor executor, @NonNull ResponseListener<T> listener) {
        executeAsync(executor, null/*use the main looper*/, listener);
    }

    @Override
    public void executeAsync(@NonNull Executor executor, @Nullable Handler handler, @NonNull ResponseListener<T> listener) {
        executor.execute(() -> {
            Handler handlerResult = handler != null ? handler : new Handler(Looper.getMainLooper());
            try {
                final T result = execute();
                handlerResult.post(() -> listener.onSuccess(result, getRequestId()));
            } catch (RequestException | ResponseException | ParseException e) {
                handlerResult.post(() -> listener.onFailed(e, getRequestId()));
            }
        });
    }

    @NonNull
    @AnyThread
    protected abstract String getUrl();

    @NonNull
    @WorkerThread
    protected abstract T parse(@NonNull InputStream inputStream, @Nullable String contentType)
            throws ParseException, UnsupportedEncodingException;

    /**
     * Content type for {@link #REQUEST_TYPE_POST} or {@link #REQUEST_TYPE_PUT}.
     */
    @Nullable
    @AnyThread
    protected String getRequestContentType() {
        return null;
    }

    /**
     * Get the request method. {@link #REQUEST_TYPE_GET} as default.
     *
     * @see #REQUEST_TYPE_GET
     * @see #REQUEST_TYPE_POST
     * @see #REQUEST_TYPE_PUT
     */
    @AnyThread
    protected String getRequestMethod() {
        return REQUEST_TYPE_GET;
    }

    /**
     * Get the connect timeout value in milliseconds.
     */
    @AnyThread
    protected int getConnectionTimeout() {
        return USE_DEFAULT_TIMEOUT;
    }

    /**
     * Get the read timeout value in milliseconds.
     */
    @AnyThread
    protected int getReadTimeout() {
        return USE_DEFAULT_TIMEOUT;
    }

    /**
     * Get the HTTP specified headers.
     */
    @AnyThread
    protected Map<String, String> getHeaders() {
        return null;
    }

    /**
     * Get the HTTP request params.
     */
    @AnyThread
    protected Map<String, String> getQuery() {
        return null;
    }

    /**
     * Writes HTTP body for {@link #REQUEST_TYPE_POST} or {@link #REQUEST_TYPE_PUT}.
     */
    @WorkerThread
    protected void writeBody(@NonNull OutputStream os) throws IOException {}

    /**
     * @return one of the {@link LogPolicy}
     */
    @MainThread
    protected int getLogPolicy() {
        return LogPolicy.ADAPTIVE;
    }

    /**
     * Get the retry request policy.
     *
     * @return instance of {@link RetryPolicy}; default {@code null}.
     * @see RetryPolicy
     */
    @MainThread
    protected RetryPolicy getRetryPolicy() {
        return null;
    }

    /**
     * Process request to server and parsing response data.
     *
     * @return Response data by typed class or throw if any problems.
     * @throws RequestException  If the request failed.
     * @throws ResponseException If the server answer with error code.
     * @throws ParseException    If the parse failed.
     */
    @NonNull
    @WorkerThread
    private T doRequest() throws RequestException, ResponseException, ParseException {

        final long startTime = SystemClock.elapsedRealtime();

        String url = null;
        int responseCode = -1;

        HttpURLConnection connection = null;

        try {
            // Apply query to url if necessary.
            url = applyQuery(getUrl());
            logcat.d(TAG, "Start http request.\n URL: %s", url);

            connection = openConnection(url);

            String requestMethod = getRequestMethod();
            connection.setRequestMethod(requestMethod);
            if (getConnectionTimeout() != USE_DEFAULT_TIMEOUT) {
                connection.setConnectTimeout(getConnectionTimeout());
            }
            if (getReadTimeout() != USE_DEFAULT_TIMEOUT) {
                connection.setReadTimeout(getReadTimeout());
            }
            connection.setDoInput(true);

            // apply headers.
            applyHeaders(connection);

            // write POST/PUT body
            if (REQUEST_TYPE_POST.equalsIgnoreCase(requestMethod) ||
                    REQUEST_TYPE_PUT.equalsIgnoreCase(requestMethod)) {
                connection.setDoOutput(true);

                String requestContentType = getRequestContentType();
                if (requestContentType != null) {
                    connection.setRequestProperty("Content-Type", requestContentType);
                }

                OutputStream os = connection.getOutputStream();
                writeBody(os);
                os.flush();
                os.close();
            }

            connection.connect();

            responseCode = connection.getResponseCode();
            logcat.d(TAG, "Request with response code: " + responseCode);
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode > HttpURLConnection.HTTP_ACCEPTED) {
                InputStream errorStream = connection.getErrorStream();
                String errorMessage = errorStream != null ? IOUtils.inputStreamToString(errorStream,
                        StandardCharsets.UTF_8) : "Unknown server error.";
                throw new ResponseException(errorMessage, responseCode);
            }

            String encoding = connection.getContentEncoding();
            InputStream inputStream = connection.getInputStream();
            if (ENCODING_GZIP.equalsIgnoreCase(encoding)) {
                inputStream = new GZIPInputStream(inputStream);
            } else if (ENCODING_DEFLATE.equalsIgnoreCase(encoding)) {
                inputStream = new InflaterInputStream(inputStream, new Inflater(true));
            }
            final T result = parse(inputStream, connection.getContentType());
            logcat.d(TAG, "Request result:\n%s", result);
            return result;

        } catch (ResponseException e) {
            if (retryPolicy != null && retryPolicy.checkNeedToRetry(e.getResponseCode())) {
                logcat.e(TAG, "Request error [code: %s]. Retry: %d",
                        e.getResponseCode(), retryPolicy.getCount());
                return doRequest();
            } else {
                throw e;
            }
        } catch (ParseException e) {
            throw e;
        } catch (Throwable e) {
            throw new RequestException(e.getMessage(), e);
        } finally {
            String format = "request took %d ms (%d)\n URL: %s ";
            long took = SystemClock.elapsedRealtime() - startTime;
            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode <= HttpURLConnection.HTTP_ACCEPTED) {
                logcat.d(TAG, format, took, responseCode, url);
            } else {
                logcat.e(TAG, format, took, responseCode, url);
            }
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Open connection to sever by specific url.
     */
    private HttpURLConnection openConnection(@NonNull String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection connection;
        switch (u.getProtocol()) {
            case SCHEME_HTTPS:
                connection = (HttpsURLConnection) u.openConnection();
                break;
            case SCHEME_HTTP:
                connection = (HttpURLConnection) u.openConnection();
                break;
            default:
                throw new MalformedURLException(
                        String.format("Unsupported protocol [%s].", u.getProtocol()));
        }
        return connection;
    }

    /**
     * Apply headers to the current connection.
     */
    private void applyHeaders(@NonNull HttpURLConnection connection) {
        Map<String, String> headers = getHeaders();
        if (headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Apply params to the current connection.
     */
    private String applyQuery(@NonNull String url) throws UnsupportedEncodingException {
        Map<String, String> query = getQuery();
        if (query == null || query.isEmpty()) {
            return url;
        }
        return mapToString(url, query);
    }

    /**
     * Convert key-value pair to byte array.
     */
    private byte[] convertBodyToBytes(Map<String, String> formUrl)
            throws UnsupportedEncodingException {
        if (formUrl == null) {
            return null;
        }
        return mapToString(null, formUrl).getBytes();
    }

    /**
     * Convert map key/value pair to {@link String}.
     */
    private String mapToString(String startWith, @NonNull Map<String, String> map)
            throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        if (startWith != null) {
            builder.append(startWith);
            if (!startWith.contains("?")) {
                builder.append('?');
            } else {
                builder.append('&');
            }
        }
        for (String key : map.keySet()) {
            builder.append(key)
                    .append("=")
                    .append(URLEncoder.encode(map.get(key), StandardCharsets.UTF_8.name()))
                    .append("&");
        }
        builder.deleteCharAt(builder.lastIndexOf("&"));
        return builder.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("HttpRequest [URL = %s]", getUrl());
    }
}
