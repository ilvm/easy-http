package xds.sample.easyhttp.network;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import xds.lib.easyhttp.HttpRequest;
import xds.lib.easyhttp.ParseException;
import xds.lib.easyhttp.util.IOUtils;
import xds.lib.easyhttp.util.RetryPolicy;

public final class JsonHttpRequest extends HttpRequest<String> {

    @NonNull
    @Override
    protected String getUrl() {
        return "https://timeapi.io/api/v1/Time/current/unix";
    }

    @Override
    protected String parseResponse(@NonNull InputStream inputStream, String contentType)
            throws ParseException {
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
        return RetryPolicy.create50x(3/*count*/, 1000L/*delay*/);
    }
}
