package xds.lib.easyhttp.util;

import android.util.Log;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import xds.lib.easyhttp.BuildConfig;

public final class TrustUtils {

    private static final String TAG = "TrustUtils";

    private TrustUtils() {}

    public static void enableTrustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {/*do nothing*/}

                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {/*do nothing*/}

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Enable trust everyone error.", e);
            }
        }
    }
}
