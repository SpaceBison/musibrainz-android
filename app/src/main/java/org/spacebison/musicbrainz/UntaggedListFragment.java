package org.spacebison.musicbrainz;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.Collection;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 15.03.16.
 */
public class UntaggedListFragment extends Fragment {
    private static final String TAG = "UntaggedListFragment";

    private final UntaggedListAdapter mAdapter = new UntaggedListAdapter();

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_untagged_list, container, false);
        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    public UntaggedListAdapter getAdapter() {
        return mAdapter;
    }

    public void setOnItemClickListener(UntaggedListAdapter.OnItemClickListener onItemClickListener) {
        mAdapter.setOnItemClickListener(onItemClickListener);
    }

    public void setOnItemLongClickListener(UntaggedListAdapter.OnItemLongClickListener onItemLongClickListener) {
        mAdapter.setOnItemLongClickListener(onItemLongClickListener);
    }

    public void setOnSectionClickListener(UntaggedListAdapter.OnSectionClickListener onSectionClickListener) {
        mAdapter.setOnSectionClickListener(onSectionClickListener);
    }

    public void setOnSectionLongClickListener(UntaggedListAdapter.OnSectionLongClickListener onSectionLongClickListener) {
        mAdapter.setOnSectionLongClickListener(onSectionLongClickListener);
    }

    public void addFiles(Collection<File> files) {
        mAdapter.addFiles(files);
    }
}
