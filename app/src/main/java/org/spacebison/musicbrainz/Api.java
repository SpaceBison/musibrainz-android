package org.spacebison.musicbrainz;

import android.util.Log;

import com.google.gson.Gson;

import org.spacebison.musicbrainz.api.Release;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by cmb on 27.03.16.
 */
public class Api {
    private static final String TAG = "Api";

    public static Release getRelease(String id) throws IOException {
        URL url = new URL("http://musicbrainz.org/ws/2/release/" + id + "?fmt=json&inc=artist-credits+recordings");
        Log.d(TAG, "URL: " + url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        InputStream is = httpURLConnection.getInputStream();
        Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String stringRelease = s.hasNext() ? s.next() : "";
        return new Gson().fromJson(stringRelease, Release.class);
    }
}
