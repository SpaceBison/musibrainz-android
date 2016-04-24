package org.spacebison.musicbrainz.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.spacebison.musicbrainz.Api;
import org.spacebison.musicbrainz.Musicbrainz;
import org.spacebison.musicbrainz.R;
import org.spacebison.musicbrainz.adapter.TaggerListAdapter;
import org.spacebison.musicbrainz.api.Release;
import org.spacebison.musicbrainz.util.Utils;

import java.io.IOException;
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



    public void loadReleaseTags(final String id) {
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

    public TaggerListAdapter getAdapter() {
        return mAdapter;
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
