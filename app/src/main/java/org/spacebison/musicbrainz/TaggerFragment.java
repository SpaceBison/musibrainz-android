package org.spacebison.musicbrainz;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.spacebison.musicbrainz.api.Release;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 28.03.16.
 */
public class TaggerFragment extends Fragment {
    private static final String TAG = "TaggerFragment";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final TaggerListAdapter mAdapter = new TaggerListAdapter();

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tagger, container, false);
        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    public void onBrowserLookupResult(final String id) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Release release = Api.getRelease(id);
                    mAdapter.addRelease(release);
                    Utils.showToast(Musicbrainz.getAppContext(), "Got tags for " + release.getTitle(), Toast.LENGTH_SHORT);
                } catch (IOException e) {
                    Utils.showToast(Musicbrainz.getAppContext(), "Could not get tags. Please try again.", Toast.LENGTH_SHORT);
                }
            }
        });
    }

    public void saveTags() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<TaggerListAdapter.ReleaseTag> releaseTags = mAdapter.getReleaseTags();
                for (TaggerListAdapter.ReleaseTag rt : releaseTags) {

                }
                return null;
            }
        }.execute();
    }

    public void setOnTrackClickListener(TaggerListAdapter.OnTrackTagClickListener onTrackTagClickListener) {
        mAdapter.setOnTrackTagClickListener(onTrackTagClickListener);
    }

    public void setOnTrackLongClickListener(TaggerListAdapter.OnTrackTagLongClickListener onTrackTagLongClickListener) {
        mAdapter.setOnTrackTagLongClickListener(onTrackTagLongClickListener);
    }

    public void setOnReleaseClickListener(TaggerListAdapter.OnReleaseTagClickListener onReleaseTagClickListener) {
        mAdapter.setOnReleaseTagClickListener(onReleaseTagClickListener);
    }

    public void setOnReleaseLongClickListener(TaggerListAdapter.OnReleaseTagLongClickListener onReleaseTagLongClickListener) {
        mAdapter.setOnReleaseTagLongClickListener(onReleaseTagLongClickListener);
    }
}
