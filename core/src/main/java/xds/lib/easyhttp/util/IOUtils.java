package xds.lib.easyhttp.util;

import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class IOUtils {

    private IOUtils() {}

    /**
     * Convert input stream to string if possible or throw {@link IOException}.
     *
     * @param is Source input stream.
     * @return String from input stream.
     */
    public static String inputStreamToString(@NonNull InputStream is) throws IOException {
        return inputStreamToString(is, Charset.forName(StandardCharsets.UTF_8.name()));
    }

    /**
     * Convert input stream to string if if possible or throw {@link IOException}.
     *
     * @param is      Source input stream.
     * @param charset Encoding format.
     * @return String from input stream.
     */
    public static String inputStreamToString(@NonNull InputStream is, Charset charset) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is, charset))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return buffer.lines().collect(Collectors.joining());
            } else {
                final StringBuilder sb = new StringBuilder();
                String line;
                while ((line = buffer.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        }
    }

    /**
     * Extracts charset from Content type according to RFC 2045
     *
     * @param contentType RFC 2045 compliant MIME string
     * @return parsed charset or {@link StandardCharsets#UTF_8}
     */
    @NonNull
    public static Charset getCharsetFromContentType(@Nullable String contentType) {
        return getCharsetFromContentType(contentType, StandardCharsets.UTF_8);
    }

    /**
     * Extracts charset from Content type according to RFC 2045
     *
     * @param contentType RFC 2045 compliant MIME string
     * @param fallback    fallback charset if parsing failed
     * @return parsed charset or fallback
     */
    @NonNull
    public static Charset getCharsetFromContentType(@Nullable String contentType, @NonNull Charset fallback) {
        if (contentType != null) {
            final String[] values = contentType.split(";");
            String charset = null;
            for (String value : values) {
                value = value.trim();
                if (value.toLowerCase().startsWith("charset=")) {
                    charset = value.substring("charset=".length());
                }
            }
            try {
                return Charset.forName(charset);
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }
}
