package xds.sample.easyhttp.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import xds.lib.easyhttp.HttpRequest;
import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.util.IOUtils;

public final class JsonHttpRequest extends HttpRequest<String> {

    private static final String HOST = "https://api.exchangeratesapi.io";
    private static final String PATH = "/latest";

    @Override
    protected String getUrl() {
        return HOST.concat(PATH);
    }

    @NonNull
    @WorkerThread
    @Override
    protected String parse(@NonNull InputStream inputStream, String contentType) throws ParseException {
        try {
            String json = IOUtils.inputStreamToString(inputStream);
            JSONObject result = new JSONObject(json);
            return result.toString(4/*indent space*/);
        } catch (IOException | JSONException e) {
            throw new ParseException(e);
        }
    }
}
