package org.spacebison.musicbrainz;

import android.app.Application;
import android.content.Context;

/**
 * Created by cmb on 28.03.16.
 */
public class Musicbrainz extends Application {
    private static Context sAppContext;

    public static Context getAppContext() {
        return sAppContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sAppContext = this;
    }
}
