package com.example.ishan.urlshortener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.model.Url;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClipboardMonitorService extends Service {
    private static final String TAG = "ClipboardManager";
    private ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    private ClipboardManager mClipboardManager;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private Urlshortener mUrlshortener;
    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: 16/6/17 Show Notification when this service is running
        mClipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mClipboardManager.addPrimaryClipChangedListener(mOnPrimaryClipChangedListener);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mUrlshortener = new Urlshortener
                .Builder(AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), null).build();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mClipboardManager != null) {
            mClipboardManager.removePrimaryClipChangedListener(mOnPrimaryClipChangedListener);
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private ClipboardManager.OnPrimaryClipChangedListener mOnPrimaryClipChangedListener =
            new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    Log.d(TAG, "onPrimaryClipChanged");
                    ClipData mClip = mClipboardManager.getPrimaryClip();
                    mThreadPool.execute(
                            new WriteHistoryRunnable(
                                    mClip.getItemAt(0).getText()
                            )
                    );
                }
            };
    private class WriteHistoryRunnable implements Runnable {
        private final Date now;
        private final CharSequence mTextToWrite;
        public WriteHistoryRunnable(CharSequence text) {
            now = new Date(System.currentTimeMillis());
            mTextToWrite = text;
        }
        @Override
        public void run() {
            if (TextUtils.isEmpty(mTextToWrite) || !URLUtil.isValidUrl(mTextToWrite.toString()))
                return;
            BufferedReader mBufferedReader;
            StringBuffer mStringBuffer;
            String res;
            String shorturl = null;
            String json = "{\"longUrl\":\"" + mTextToWrite + "\"}";
            try {
                URL url =
                        new URL("https://www.googleapis.com/urlshortener/v1/url?key=AIzaSyA87S0WS8jDb2CJDkC9oPI0PcWVAMNx5Z0");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(40000);
                con.setConnectTimeout(40000);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                OutputStream os = con.getOutputStream();
                BufferedWriter writer =
                        new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(json);
                writer.flush();
                writer.close();
                os.close();
                int status = con.getResponseCode();
                InputStream inputStream;
                if (status == HttpURLConnection.HTTP_OK)
                    inputStream = con.getInputStream();
                else
                    inputStream = con.getErrorStream();
                mBufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                mStringBuffer = new StringBuffer();
                String line = "";
                while ((line=mBufferedReader.readLine())!=null)
                    mStringBuffer.append(line);
                res = mStringBuffer.toString();
                try {
                    JSONObject jsonObject = new JSONObject(res);
                    shorturl = jsonObject.get("id").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "JSON!0");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.d(TAG, "MalformedURLException");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "IO");
            }
            mDatabase.child("users").child(currentUser.getUid()).push().setValue(mTextToWrite);
            mClipboardManager.setPrimaryClip(ClipData.newRawUri("", Uri.parse(shorturl)));
            notification();
        }
    }
    private void notification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.common_google_signin_btn_text_light_pressed)
                .setContentText("Link Shortened")
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(true);
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(209, mBuilder.build());
    }
}
