package xds.lib.easyhttp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;

import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.exception.RequestException;
import xds.lib.easyhttp.exception.ResponseException;
import xds.lib.easyhttp.util.IOUtils;
import xds.lib.easyhttp.util.RetryPolicy;

/**
 * Abstract base class for making HTTP requests with customizable parameters.
 *
 * @param <T> The type of response expected from the request.
 */
public abstract class HttpRequest<T> implements Request<T> {

    protected static final String METHOD_GET = "GET";
    protected static final String METHOD_POST = "POST";
    protected static final String METHOD_PUT = "PUT";

    private static final int NOT_SET = -1;
    private static final int DEFAULT_MAX_REDIRECTS = 1;

    private static final String ENCODING_GZIP = "gzip";
    private static final String ENCODING_DEFLATE = "deflate";

    private final RetryPolicy retryPolicy;

    /** Default constructor for HttpRequest. */
    protected HttpRequest() {
        this.retryPolicy = createRetryPolicy();
    }

    /** {@inheritDoc} */
    public final T execute() throws RequestException, ResponseException, ParseException {
        try {
            return executeRequest(getUrl(), 0, System.nanoTime());
        } catch (IOException e) {
            throw new RequestException("IO error during request execution", e);
        }
    }

    /**
     * Returns the URL for the HTTP request.
     *
     * @return The URL as a String.
     */
    protected abstract String getUrl();

    /**
     * Parses the HTTP response into the desired format.
     *
     * @param inputStream The input stream containing the response body.
     * @param info Response metadata such as content type, content length,
     * and request execution time.
     * @return The parsed response of type {@code T}.
     * @throws ParseException If an error occurs while parsing the response.
     * @throws IOException If an I/O error occurs while reading the response.
     */
    protected abstract T parseResponse(InputStream inputStream, ResponseInfo info)
            throws ParseException, IOException;

    /**
     * Returns the HTTP method for the request (e.g., GET, POST, PUT).
     *
     * @return The HTTP method as a String.
     */
    protected String getRequestMethod() {
        return METHOD_GET;
    }

    /**
     * Returns the connection timeout in milliseconds.
     *
     * @return The connection timeout in milliseconds.
     */
    protected int getConnectionTimeout() {
        return NOT_SET;
    }

    /**
     * Returns the read timeout in milliseconds.
     *
     * @return The read timeout in milliseconds.
     */
    protected int getReadTimeout() {
        return NOT_SET;
    }

    /**
     * Returns the headers to be included in the HTTP request.
     *
     * @return A map of header names to header values.
     */
    protected Map<String, String> getHeaders() {
        return null;
    }

    /**
     * Returns the query parameters to be included in the URL.
     *
     * @return A map of query parameter names to parameter values.
     */
    protected Map<String, String> getQueryParameters() {
        return null;
    }

    /**
     * Returns the content type for the request, if applicable.
     *
     * @return The content type as a String, or null if not applicable.
     */
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
    protected void writeRequestBody(OutputStream os) throws IOException {
        // Default implementation does nothing
    }

    /**
     * Creates the retry policy for the request.
     *
     * @return The retry policy, or null if no retry policy is needed.
     */
    protected RetryPolicy createRetryPolicy() {
        return null;
    }

    /**
     * Returns the maximum number of redirects allowed for this request.
     * This method can be overridden by subclasses to customize the redirect depth.
     *
     * @return The maximum number of redirects.
     */
    protected int getMaxRedirects() {
        return DEFAULT_MAX_REDIRECTS;
    }


    /**
     * Called when an HTTP response status code is received.
     *
     * @param url The URL that was requested
     * @param statusCode The HTTP response status code (e.g., 200, 404, 500)
     * @return {@code true} if further processing of the request should be aborted;
     * {@code false} to continue with default processing
     */
    protected boolean onResponseStatus(String url, int statusCode) {
        return false;
    }

    /**
     * Executes the HTTP request and handles redirects, if necessary.
     *
     * @param redirectCount The current redirect count.
     * @return The parsed response of type {@code T}.
     * @throws IOException If an I/O error occurs.
     * @throws RequestException If there is an issue with the request.
     * @throws ResponseException If the server returns an error.
     * @throws ParseException If there is an error parsing the response.
     */
    private T executeRequest(String url, int redirectCount, long startNs)
            throws IOException, RequestException, ResponseException, ParseException {
        if (redirectCount > getMaxRedirects()) {
            throw new RequestException("Too many redirects");
        }

        HttpURLConnection connection = null;

        try {
            final String requestUrl = buildRequestUrl(url);

            connection = openConnection(requestUrl);
            setupConnection(connection);
            connection.connect();

            final int responseCode = connection.getResponseCode();

            final long rtt = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            if (onResponseStatus(url, responseCode)) {
                throw new RequestException(
                        "Request processing was aborted due to response status: " + responseCode);
            }
            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream inputStream = getInputStream(connection)) {
                    return parseResponse(
                            inputStream,
                            new ResponseInfo(
                                    connection.getContentType(), connection.getContentLength(), rtt
                            )
                    );
                }
            } else if (responseCode >= HttpURLConnection.HTTP_MULT_CHOICE &&
                    responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                // Handling redirection
                final String newUrl = connection.getHeaderField("Location");
                if (newUrl == null) {
                    throw new ResponseException("Redirected without a new location", responseCode);
                }
                return executeRequest(newUrl, redirectCount + 1, startNs);
            } else {
                String errorMessage = getErrorMessage(connection);
                throw new ResponseException(
                        "HTTP error: " + responseCode +
                                (errorMessage != null ? ".\n" + errorMessage : ""),
                        responseCode
                );
            }
        } catch (IOException | ResponseException e) {
            if (retryPolicy != null && retryPolicy.checkNeedToRetry(e)) {
                return executeRequest(url, 0, startNs);
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
        URL url = URI.create(requestUrl).toURL();
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
     */
    private String getErrorMessage(HttpURLConnection connection) {
        try (InputStream errorStream = connection.getErrorStream()) {
            return errorStream != null ?
                    IOUtils.readString(errorStream, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns a string representation of the HttpRequest, primarily for debugging purposes.
     *
     * @return A string representation of the HttpRequest.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [URL = " + getUrl() + "]";
    }

    /**
     * Contains metadata associated with an HTTP response.
     */
    protected static final class ResponseInfo {

        private final String contentType;
        private final int contentLength;
        private final long rtt;

        private ResponseInfo(String contentType, int contentLength, long rtt) {
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.rtt = rtt;
        }

        /**
         * The value of the {@code Content-Type} response header,
         * or {@code null} if not specified by the server.
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * The response content length in bytes as reported by the server,
         * or {@code -1} if the length is unknown.
         */
        public int getContentLength() {
            return contentLength;
        }

        /**
         * The total time spent executing the request in milliseconds.
         * Includes connection establishment, request transmission,
         * server processing, and receiving response headers.
         */
        public long getRtt() {
            return rtt;
        }
    }
}
