package org.spacebison.musicbrainz.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.spacebison.musicbrainz.Api;
import org.spacebison.musicbrainz.MainActivity;
import org.spacebison.musicbrainz.R;
import org.spacebison.musicbrainz.api.Release;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by cmb on 22.04.16.
 */
public class WebServerService extends Service implements Handler.Callback {
    public static final int MESSAGE_GET_PORT = 1;
    public static final int RESPONSE_GET_PORT = MESSAGE_GET_PORT;
    private static final String TAG = "WebServerService";
    private final Handler mHandler = new Handler(Looper.getMainLooper(), this);
    private final Messenger mMessenger = new Messenger(mHandler);
    private WebServer mWebServer;

    @Override
    public void onCreate() {
        super.onCreate();
        initServer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mWebServer != null && mWebServer.isAlive()) {
            mWebServer.stop();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what != MESSAGE_GET_PORT) {
            return false;
        }

        if (msg.replyTo == null) {
            return true;
        }

        if (mWebServer == null) {
            initServer();
        }

        final Message reply = Message.obtain();
        reply.what = RESPONSE_GET_PORT;
        reply.arg1 = mWebServer.getListeningPort();

        try {
            msg.replyTo.send(reply);
        } catch (RemoteException e) {
            Crashlytics.logException(e);
        }

        return true;
    }

    private synchronized void initServer() {
        do {
            try {
                final ServerSocket serverSocket = new ServerSocket(0);
                final int localPort = serverSocket.getLocalPort();
                Log.d(TAG, "Trying port " + localPort + " for web server");
                serverSocket.close();
                mWebServer = new WebServer(localPort);
                Log.d(TAG, "Starting web server");
                mWebServer.start();
                Log.d(TAG, "Started on port " + localPort);
            } catch (IOException e) {
                Crashlytics.logException(e);
            }
        } while (mWebServer == null);
    }

    private class WebServer extends NanoHTTPD {
        public WebServer(int port) {
            super(port);
        }

        public WebServer(String hostname, int port) {
            super(hostname, port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> params = session.getParms();
            Log.d(TAG, "Serve session; params: " + params);

            final String id = params.get("id");

            try {
                Release release = Api.getRelease(id);

                final Intent intent = new Intent(WebServerService.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_RELEASE, (Parcelable) release);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
            } catch (IOException e) {
                Log.w(TAG, "Could not get release", e);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WebServerService.this, R.string.could_not_get_tags, Toast.LENGTH_LONG).show();
                    }
                });
            }

            return super.serve(session);
        }
    }
}
