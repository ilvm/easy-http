package xds.lib.easyhttp;

import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;

import xds.lib.easyhttp.async.ResponseListener;
import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.exception.RequestException;
import xds.lib.easyhttp.exception.ResponseException;
import xds.lib.easyhttp.util.IOUtils;
import xds.lib.easyhttp.util.LogPolicy;
import xds.lib.easyhttp.util.RetryPolicy;

/**
 * Abstract base class for making HTTP requests with customizable parameters.
 *
 * @param <T> The type of response expected from the request.
 */
public abstract class HttpRequest<T> implements Request<T> {

    protected final String TAG = getClass().getSimpleName();

    protected static final String METHOD_GET = "GET";
    protected static final String METHOD_POST = "POST";
    protected static final String METHOD_PUT = "PUT";

    private static final int NOT_SET = -1;
    private static final int DEFAULT_MAX_REDIRECTS = 1;

    private static final String ENCODING_GZIP = "gzip";
    private static final String ENCODING_DEFLATE = "deflate";

    private static final String LOG_RESULT_FORMAT = "Request took %d ms\n URL: %s";
    private static final String LOG_ERROR_FORMAT = "Request error took %d ms\n URL: %s";

    private final RetryPolicy retryPolicy;
    private final Logcat logcat;

    /** Default constructor for HttpRequest. */
    protected HttpRequest() {
        this.retryPolicy = createRetryPolicy();
        this.logcat = new Logcat(getLogPolicy());
    }

    /** {@inheritDoc} */
    @WorkerThread
    public final T execute() throws RequestException, ResponseException, ParseException {
        try {
            return executeRequest(getUrl(), 0);
        } catch (IOException e) {
            throw new RequestException("IO error during request execution", e);
        }
    }

    /** {@inheritDoc} */
    public final void executeAsync(@NonNull Executor executor, Handler handler,
            @NonNull ResponseListener<T> listener) {
        executor.execute(() -> {

            final long startTime = SystemClock.elapsedRealtime();
            try {
                final T result = execute();
                logcat.d(TAG, LOG_RESULT_FORMAT,
                        (SystemClock.elapsedRealtime() - startTime), getUrl());
                postToHandler(handler, () -> listener.onSuccess(result, getRequestId()));
            } catch (RequestException | ResponseException | ParseException e) {
                logcat.e(TAG, LOG_ERROR_FORMAT,
                        (SystemClock.elapsedRealtime() - startTime), getUrl());
                postToHandler(handler, () -> listener.onFailed(e, getRequestId()));
            }
        });
    }

    /**
     * Returns the URL for the HTTP request.
     *
     * @return The URL as a String.
     */
    @NonNull
    @AnyThread
    protected abstract String getUrl();

    /**
     * Parses the HTTP response into the desired format.
     *
     * @param inputStream The input stream containing the HTTP response data.
     * @param contentType The content type of the response.
     * @return The parsed response of type {@code T}.
     * @throws ParseException If there is an error parsing the response.
     * @throws IOException If an I/O error occurs.
     */
    @WorkerThread
    protected abstract T parseResponse(@NonNull InputStream inputStream, String contentType)
            throws ParseException, IOException;

    /**
     * Returns the HTTP method for the request (e.g., GET, POST, PUT).
     *
     * @return The HTTP method as a String.
     */
    @AnyThread
    protected String getRequestMethod() {
        return METHOD_GET;
    }

    /**
     * Returns the connection timeout in milliseconds.
     *
     * @return The connection timeout in milliseconds.
     */
    @AnyThread
    protected int getConnectionTimeout() {
        return NOT_SET;
    }

    /**
     * Returns the read timeout in milliseconds.
     *
     * @return The read timeout in milliseconds.
     */
    @AnyThread
    protected int getReadTimeout() {
        return NOT_SET;
    }

    /**
     * Returns the headers to be included in the HTTP request.
     *
     * @return A map of header names to header values.
     */
    @AnyThread
    protected Map<String, String> getHeaders() {
        return null;
    }

    /**
     * Returns the query parameters to be included in the URL.
     *
     * @return A map of query parameter names to parameter values.
     */
    @AnyThread
    protected Map<String, String> getQueryParameters() {
        return null;
    }

    /**
     * Returns the content type for the request, if applicable.
     *
     * @return The content type as a String, or null if not applicable.
     */
    @Nullable
    @AnyThread
    protected String getRequestContentType() {
        return null;
    }

    /**
     * Writes the request body to the provided output stream.
     * This method is only used for POST and PUT requests.
     *
     * @param os The output stream to write the body to.
     * @throws IOException If an I/O error occurs.
     */
    @WorkerThread
    protected void writeRequestBody(@NonNull OutputStream os) throws IOException {
        // Default implementation does nothing
    }

    /**
     * Returns the logging policy for the request.
     *
     * @return The logging policy as an integer.
     */
    @MainThread
    protected int getLogPolicy() {
        return LogPolicy.ADAPTIVE;
    }

    /**
     * Creates the retry policy for the request.
     *
     * @return The retry policy, or null if no retry policy is needed.
     */
    @MainThread
    protected RetryPolicy createRetryPolicy() {
        return null;
    }

    /**
     * Returns the maximum number of redirects allowed for this request.
     * This method can be overridden by subclasses to customize the redirect depth.
     *
     * @return The maximum number of redirects.
     */
    @AnyThread
    protected int getMaxRedirects() {
        return DEFAULT_MAX_REDIRECTS;
    }

    /**
     * // Default value
     * Executes the HTTP request and handles redirects, if necessary.
     *
     * @param redirectCount The current redirect count.
     * @return The parsed response of type {@code T}.
     * @throws IOException If an I/O error occurs.
     * @throws RequestException If there is an issue with the request.
     * @throws ResponseException If the server returns an error.
     * @throws ParseException If there is an error parsing the response.
     */
    private T executeRequest(String url, int redirectCount)
            throws IOException, RequestException, ResponseException, ParseException {
        if (redirectCount > getMaxRedirects()) {
            throw new RequestException("Too many redirects");
        }

        HttpURLConnection connection = null;

        try {
            final String requestUrl = buildRequestUrl(url);
            logcat.d(TAG, "Executing request: %s", requestUrl);

            connection = openConnection(requestUrl);
            setupConnection(connection);
            connection.connect();

            final int responseCode = connection.getResponseCode();
            if (responseCode >= HttpURLConnection.HTTP_OK &&
                    responseCode <= HttpURLConnection.HTTP_ACCEPTED) {
                try (InputStream inputStream = getInputStream(connection)) {
                    return parseResponse(inputStream, connection.getContentType());
                }
            } else if (responseCode >= HttpURLConnection.HTTP_MULT_CHOICE &&
                    responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                // Handling redirection
                final String newUrl = connection.getHeaderField("Location");
                if (newUrl == null) {
                    throw new ResponseException("Redirected without a new location", responseCode);
                }
                logcat.d(TAG, "Redirecting to: %s", newUrl);
                return executeRequest(newUrl, redirectCount + 1);
            } else {
                throw new ResponseException(getErrorMessage(connection), responseCode);
            }
        } catch (IOException | RequestException | ResponseException e) {
            if (retryPolicy != null && retryPolicy.checkNeedToRetry(e)) {
                logcat.w(TAG, "Request error, retry: %d\n%s", retryPolicy.getCount(), e);
                return executeRequest(url, 0);
            } else {
                throw e;
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Opens an HTTP connection to the specified URL.
     *
     * @param requestUrl The URL to connect to.
     * @return An instance of HttpURLConnection.
     * @throws IOException If an I/O error occurs.
     */
    private HttpURLConnection openConnection(String requestUrl) throws IOException {
        URL url = new URL(requestUrl);
        if ("https".equalsIgnoreCase(url.getProtocol())) {
            return (HttpsURLConnection) url.openConnection();
        } else if ("http".equalsIgnoreCase(url.getProtocol())) {
            return (HttpURLConnection) url.openConnection();
        } else {
            throw new MalformedURLException("Unsupported protocol: " + url.getProtocol());
        }
    }

    /**
     * Configures the HTTP connection with the appropriate settings, such as method, timeouts,
     * and headers.
     *
     * @param connection The HttpURLConnection to configure.
     * @throws IOException If an I/O error occurs.
     */
    private void setupConnection(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod(getRequestMethod());
        if (getConnectionTimeout() > 0) {
            connection.setConnectTimeout(getConnectionTimeout());
        }
        if (getReadTimeout() > 0) {
            connection.setReadTimeout(getReadTimeout());
        }
        connection.setDoInput(true);
        applyHeaders(connection);

        if (isBodyRequired()) {
            connection.setDoOutput(true);
            if (getRequestContentType() != null) {
                connection.setRequestProperty("Content-Type", getRequestContentType());
            }
            try (OutputStream os = connection.getOutputStream()) {
                writeRequestBody(os);
            }
        }
    }

    /**
     * Determines if the request requires a request body (i.e., for POST or PUT methods).
     *
     * @return True if the request method is POST or PUT, otherwise false.
     */
    private boolean isBodyRequired() {
        String method = getRequestMethod();
        return METHOD_POST.equalsIgnoreCase(method) || METHOD_PUT.equalsIgnoreCase(method);
    }

    /**
     * Applies headers to the HTTP connection.
     *
     * @param connection The HttpURLConnection to apply headers to.
     */
    private void applyHeaders(HttpURLConnection connection) {
        final Map<String, String> headers = getHeaders();
        if (headers == null) {
            return;
        }
        for (String key : headers.keySet()) {
            connection.setRequestProperty(key, headers.get(key));
        }
    }

    /**
     * Returns the input stream for the HTTP connection, handling any necessary decompression.
     *
     * @param connection The HttpURLConnection to read the input stream from.
     * @return The input stream, potentially wrapped in a decompression stream.
     * @throws IOException If an I/O error occurs.
     */
    private InputStream getInputStream(HttpURLConnection connection) throws IOException {
        String encoding = connection.getContentEncoding();
        InputStream inputStream = connection.getInputStream();
        if (ENCODING_GZIP.equalsIgnoreCase(encoding)) {
            return new GZIPInputStream(inputStream);
        } else if (ENCODING_DEFLATE.equalsIgnoreCase(encoding)) {
            return new InflaterInputStream(inputStream, new Inflater(true));
        } else {
            return inputStream;
        }
    }

    /**
     * Builds the full request URL, including query parameters.
     *
     * @return The full request URL as a String.
     * @throws IOException If an encoding error occurs.
     */
    private String buildRequestUrl(String url) throws IOException {

        final Map<String, String> queryParams = getQueryParameters();
        if (queryParams == null) {
            return url;
        }

        final StringBuilder urlBuilder = new StringBuilder(url);
        if (!queryParams.isEmpty()) {
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()))
                        .append("&");
            }
            urlBuilder.setLength(urlBuilder.length() - 1); // remove trailing "&"
        }

        return urlBuilder.toString();
    }

    /**
     * Retrieves the error message from the HTTP connection if the request failed.
     *
     * @param connection The HttpURLConnection to retrieve the error message from.
     * @return The error message as a String.
     * @throws IOException If an I/O error occurs.
     */
    private String getErrorMessage(HttpURLConnection connection) throws IOException {
        try (InputStream errorStream = connection.getErrorStream()) {
            return errorStream != null ?
                    IOUtils.inputStreamToString(errorStream, StandardCharsets.UTF_8) :
                    "Unknown server error";
        }
    }

    /**
     * Posts a task to the provided handler, or runs it immediately if the handler is null.
     *
     * @param handler The handler to post the task to.
     * @param task The task to run.
     */
    private static void postToHandler(@Nullable Handler handler, Runnable task) {
        if (handler != null) {
            handler.post(task);
        } else {
            task.run();
        }
    }

    /**
     * Returns a string representation of the HttpRequest, primarily for debugging purposes.
     *
     * @return A string representation of the HttpRequest.
     */
    @NonNull
    @Override
    public String toString() {
        return String.format("HttpRequest [URL = %s]", getUrl());
    }
}
