package xds.sample.easyhttp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.TextView;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xds.lib.easyhttp.async.ResponseListener;
import xds.lib.easyhttp.exception.ParseException;
import xds.lib.easyhttp.exception.RequestException;
import xds.lib.easyhttp.exception.ResponseException;
import xds.sample.easyhttp.network.JsonHttpRequest;

public final class SampleActivity extends Activity {

    private TextView textView;
    private Executor executor = new MyExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        textView = findViewById(R.id.text_response);
        findViewById(R.id.btn_request).setOnClickListener((view) -> new Thread(() -> {
            try {
                String response = new JsonHttpRequest().execute();
                textView.post(() -> textView.setText(response));
            } catch (RequestException | ResponseException | ParseException e) {
                textView.post(() -> textView.setText(e.getMessage()));
            }
        }).start());

        findViewById(R.id.btn_request_async).setOnClickListener((view) ->
                new JsonHttpRequest().executeAsync(executor, responseListener));
    }

    private final ResponseListener<String> responseListener = new ResponseListener<String>() {
        @Override
        public void onSuccess(@NonNull String response, @NonNull String requestId) {
            textView.setText(String.format(getString(R.string.txt_result_format), requestId, response));
        }

        @Override
        public void onFailed(@NonNull Throwable exception, @NonNull String requestId) {
            textView.setText(String.format(getString(R.string.txt_result_format), requestId, exception.getMessage()));
        }
    };

    private static class MyExecutor implements Executor {

        private final Handler handler;

        MyExecutor() {
            final HandlerThread thread = new HandlerThread(MyExecutor.class.getSimpleName());
            thread.start();
            handler = new Handler(thread.getLooper());
        }

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
