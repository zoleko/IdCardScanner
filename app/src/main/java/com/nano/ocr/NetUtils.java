package com.nano.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetUtils {
    private static final String TAG = NetUtils.class.getName();

    public static final int HTTP_TIMEOUT = 1 * 20 * 1000; // 20 seconds

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager mngr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mngr.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            return true;
        }
        return false;
    }

    public static String post(String address, Map<String, String> headers, String requestBody) {
        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL(address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            //set the headers
            connection.setRequestProperty("Content-Type","application/json");
            if (headers != null) {
                for (String header : headers.keySet()) {
                    connection.setRequestProperty(header, headers.get(header));
                }
            }

            if(requestBody!=null){
                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                OutputStreamWriter writer = new OutputStreamWriter(out);
                writer.write(requestBody, 0, requestBody.length());

                writer.close();
            }


            InputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = "";

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            reader.close();
            if (connection != null) {
                connection.disconnect();
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return buffer.toString();

    }

    static public InputStream postMultiPart(String url, HashMap<String,String> params, InputStream fileStream) throws IOException {
        InputStream stream=PostMultiPart.multipartRequest(url,params
                ,fileStream,"file","image/jpeg");
        return stream;

    }

    public static String toBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray,   Base64.URL_SAFE);

    }

    public static String toBase642(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray,  Base64.URL_SAFE);
    }

    public static File bitmapTofile(Context context,Bitmap bitmap) throws IOException {
        File f = new File(context.getCacheDir(), "tmpocr.bmp");
        f.createNewFile();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(bitmapdata);
        fos.flush();
        fos.close();
        return f;
    }


    /////////////////

    public static String buildJsonParams(String... params) {
        String fieldString = "";
        //Uri.Builder builder = new Uri.Builder();
        try {
            try {
                Log.e("WV", "url : " + params[0]);
            } catch (Exception e) {
            }
            int count = params.length;
            int i = 1;
            JSONObject j = new JSONObject();
            while (i <= (count - 1)) {

                j.put(params[i], params[i + 1]);
                Log.e("WV", "key : " + params[i] + "  value : " + params[i + 1]);
                i = i + 2;
            }

            fieldString = j.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        Log.e("WV", "json param : " + fieldString);
        return fieldString;
    }


    public static WvHttpResponse getServerResponse(String uri, String[] params, int timeout, String method, String token) {

        WvHttpResponse WvHttpResponse = new WvHttpResponse(0, null, "", false, "");
        if (timeout == 0) timeout = HTTP_TIMEOUT;
        HttpURLConnection conn = null;
        BufferedReader br = null;
        try {

            String parameters = "";

                    if(params[1].startsWith("{")){
                        parameters=params[1];
                    }else {
                        parameters = buildJsonParams(params);
                    }
                    URL url = new URL(uri);

                    if (url.getProtocol().toLowerCase(Locale.US).equals("https")) {
                        trustAllHosts();
                        HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                        https.setHostnameVerifier(DO_NOT_VERIFY);
                        conn = https;
                    } else {
                        conn = (HttpURLConnection) url.openConnection();
                    }

                    conn.setUseCaches(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setFixedLengthStreamingMode(parameters.length());
                    conn.setRequestProperty("Content-Type", "application/json");
                    // conn.setRequestProperty("Content-Language", Locale.getDefault().getLanguage());
                    conn.setConnectTimeout(timeout);
                    conn.setReadTimeout(timeout);
                    conn.setRequestProperty("connection", "close");
                    if(!TextUtils.isEmpty(token)){

                        conn.setRequestProperty("Authorization", "Bearer " +token);

                    }
                    Log.e("parameters","json parameters:"+parameters+"|token:"+token);

            if(!method.equals("get")) { // no body for get
                try {
                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(parameters);
                    writer.flush();
                    writer.close();
                    os.close();
                } catch (final java.net.SocketTimeoutException e) {
                    // connection timed out...let's try again
                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(parameters);
                    writer.flush();
                    writer.close();
                    os.close();
                }
            }
            //  conn.connect();


	          /*  if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
	                throw new RuntimeException("Failed : HTTP error code : "
	                        + conn.getResponseCode());
	            }*/
            InputStreamReader in;


            int status = conn.getResponseCode();

            WvHttpResponse.setCode(status);

            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
                WvHttpResponse.setIs200ok(true);
                in = new InputStreamReader((conn.getInputStream()));

                //throw new Exception("Bad authentication status: " + status);
            } else {
                WvHttpResponse.setIs200ok(false);
                in = new InputStreamReader((conn.getErrorStream()));
            }

            br = new BufferedReader(in);

            int c;
            StringBuilder response = new StringBuilder();

            while ((c = br.read()) != -1) {
                //Since c is an integer, cast it to a char. If it isn't -1, it will be in the correct range of char.
                response.append((char) c);
            }
            String result = response.toString();
            WvHttpResponse.setResponse(result.trim());


            conn.disconnect();


        } catch (MalformedURLException e) {
            e.printStackTrace();
            WvHttpResponse.setCode(-2);
            WvHttpResponse.setError(e);
            WvHttpResponse.setErrorMessage("MalformedURLException");
        } catch (IOException e) {
            e.printStackTrace();
            WvHttpResponse.setError(e);
            WvHttpResponse.setCode(-2);
            if (e instanceof SocketTimeoutException) {
                WvHttpResponse.setErrorMessage("SocketTimeoutException");
            } else {
                WvHttpResponse.setErrorMessage("IOException");
            }


        } catch (Exception e) {
            WvHttpResponse.setCode(-2);
            e.printStackTrace();
            WvHttpResponse.setError(e);
            WvHttpResponse.setErrorMessage("Request Exception");
        } finally {

            //close input
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ioex) {
                    //Very bad things just happened... handle it
                }
            }

            if (conn != null) {
                conn.disconnect();
            }
        }
        return WvHttpResponse;
    }


    // do not verify the host - dont check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
