package org.spacebison.musicbrainz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 15.03.16.
 */
public class UntaggedListFragment extends Fragment {
    private static final String TAG = "UntaggedListFragment";
    private static final int CHOOSE_FILE_REQUEST_CODE = 1;

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private final UntaggedListAdapter mAdapter = new UntaggedListAdapter();
    private final RecyclerViewAdapterNotifier mAdapterNotifier = new RecyclerViewAdapterNotifier(mAdapter);
    private int mPort = 8000;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_untagged_list, container, false);
        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnSectionLongClickListener(new UntaggedListAdapter.OnSectionLongClickListener() {
            @Override
            public void onSectionLongClick(UntaggedListAdapter adapter, final UntaggedListAdapter.Section section) {
                CharSequence[] items = new CharSequence[] { "Search in browser" };
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Uri.Builder uriBuilder = new Uri.Builder()
                                        .scheme("http")
                                        .authority("musicbrainz.org")
                                        .appendPath("taglookup")
                                        .appendQueryParameter("tport", Integer.toString(mPort)) // // TODO: 27.03.16 add working port
                                        .appendQueryParameter("tag-lookup.release", section.album)
                                        .appendQueryParameter("tag-lookup.artist", section.artist);
                                int color = ContextCompat.getColor(getContext(), R.color.colorPrimary);
                                startActivity(Utils.getChromeCustomTabIntent(uriBuilder.build(), color));
                        }
                    }
                });
                builder.show();
            }
        });

        return view;
    }

    public void showAddFilesScreen() {
        final Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, FilePickerActivity.class);
            intent.putExtra(FilePickerActivity.EXTRA_DIR, Environment.getExternalStorageDirectory());
            startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Set<File> chosenFiles = (Set<File>) data.getSerializableExtra(FilePickerActivity.EXTRA_FILES);
            mAdapter.addFiles(chosenFiles);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setPort(int port) {
        mPort = port;
    }
}
