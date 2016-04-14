package org.spacebison.musicbrainz;

import org.spacebison.progressviewcontroller.ProgressViewController;

/**
 * Created by cmb on 11.04.16.
 */
public class TaskProgressListener implements ProgressListener {
    private final ProgressViewController mProgressViewController;
    private final String mId;

    public TaskProgressListener(ProgressViewController progressViewController, String id) {
        mProgressViewController = progressViewController;
        mId = id;
    }

    @Override
    public void onStarted() {
        mProgressViewController.notifyTaskStarted(mId);
    }

    @Override
    public void onProgressChanged(int progress, int maxProgress) {
        mProgressViewController.notifyTaskProgressChanged(mId, progress, maxProgress);
    }

    @Override
    public void onFinished() {
        mProgressViewController.notifyTaskFinished(mId);
    }
}
