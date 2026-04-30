package xds.lib.easyhttp.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class IOUtils {

    private IOUtils() {}

    /**
     * Reads the entire input stream and converts it to a String.
     *
     * @param is source input stream
     * @return stream content as a String
     * @throws IOException if reading fails
     */
    public static String readString(InputStream is) throws IOException {
        return readString(is, StandardCharsets.UTF_8);
    }

    /**
     * Reads the entire input stream and converts it to a String.
     *
     * @param is source input stream
     * @param charset character encoding
     * @return stream content as a String
     * @throws IOException if reading fails
     */
    public static String readString(InputStream is, Charset charset)
            throws IOException {
        return readString(is, -1, charset);
    }

    /**
     * Reads the entire input stream and converts it to a String.
     *
     * @param is source input stream
     * @param contentLength expected content length in bytes, or -1 if unknown
     * @param charset character encoding
     * @return stream content as a String
     * @throws IOException if reading fails
     */
    public static String readString(InputStream is, int contentLength, Charset charset)
            throws IOException {
        try (Reader reader = new InputStreamReader(is, charset)) {
            StringBuilder sb = contentLength > 0
                    ? new StringBuilder(contentLength)
                    : new StringBuilder();
            char[] buffer = new char[8192];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, n);
            }
            return sb.toString();
        }
    }

    /**
     * Reads the entire input stream and converts it to a long.
     *
     * @param is source input stream
     * @return stream content as a long
     * @throws IOException if reading fails
     */
    public static long readLong(InputStream is) throws IOException {
        int c = is.read();
        if (c == -1) {
            throw new NumberFormatException("Cannot parse long from InputStream: stream is empty");
        }
        boolean negative = c == '-';
        long result = 0;
        if (!negative) {
            if (c < '0' || c > '9') {
                throw new NumberFormatException(
                        "Cannot parse long from InputStream: invalid character '" + (char) c + "'"
                );
            }
            result = c - '0';
        }
        while ((c = is.read()) != -1) {
            if (c < '0' || c > '9') {
                throw new NumberFormatException(
                        "Cannot parse long from InputStream: invalid character '" + (char) c + "'"
                );
            }
            result = result * 10 + (c - '0');
        }
        return negative ? -result : result;
    }
}
