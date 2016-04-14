package org.spacebison.musicbrainz;

/**
 * Created by cmb on 11.04.16.
 */
public interface ProgressListener {
    void onStarted();
    void onProgressChanged(int progress, int maxProgress);
    void onFinished();
}
