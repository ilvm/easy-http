package xds.sample.easyhttp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.exception.RequestException;
import xds.lib.easyhttp.exception.ResponseException;
import xds.sample.easyhttp.network.JsonHttpRequest;

public final class SampleActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        final TextView textView = findViewById(R.id.text_response);
        findViewById(R.id.btn_request).setOnClickListener((view) -> {
            new Thread(() -> {
                try {
                    String response = new JsonHttpRequest().execute();
                    textView.post(() -> textView.setText(response));
                } catch (RequestException | ResponseException | ParseException e) {
                    textView.post(() -> textView.setText(e.getMessage()));
                }
            }).start();
        });
    }
}
