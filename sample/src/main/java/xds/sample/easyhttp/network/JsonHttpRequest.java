package xds.sample.easyhttp.network;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import xds.lib.easyhttp.HttpRequest;
import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.util.IOUtils;
import xds.lib.easyhttp.util.RetryPolicy;

public final class JsonHttpRequest extends HttpRequest<String> {

    private static final String HOST = "https://apidata.mos.ru";
    private static final String PATH = "/version";

    @NonNull
    @Override
    protected String getUrl() {
        return HOST.concat(PATH);
    }

    @Override
    protected String parseResponse(@NonNull InputStream inputStream, String contentType)
            throws ParseException, IOException {
        try {
            String json = IOUtils.inputStreamToString(inputStream);
            JSONObject result = new JSONObject(json);
            return result.toString(4/*indent space*/);
        } catch (IOException | JSONException e) {
            throw new ParseException(e);
        }
    }

    @Override
    protected RetryPolicy createRetryPolicy() {
        return RetryPolicy.create500();
    }
}
