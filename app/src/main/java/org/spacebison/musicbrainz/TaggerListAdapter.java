package org.spacebison.musicbrainz;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.spacebison.musicbrainz.api.Medium;
import org.spacebison.musicbrainz.api.Release;
import org.spacebison.musicbrainz.api.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 09.03.16.
 */
public class TaggerListAdapter extends RecyclerView.Adapter<TaggerListAdapter.ParentViewHolder> {
    private static final String TAG = "UntaggedListAdapter";
    private final OrderedHashSet<Release> mReleases = new OrderedHashSet<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final LinkedList<ChildAdapter> mChildAdapters = new LinkedList<>();
    private final RecyclerViewAdapterNotifier mNotifier = new RecyclerViewAdapterNotifier(this);
    private OnTrackClickListener mOnTrackClickListener;
    private OnTrackLongClickListener mOnTrackLongClickListener;
    private OnReleaseClickListener mOnReleaseClickListener;
    private OnReleaseLongClickListener mOnReleaseLongClickListener;

    @Override
    public ParentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.item_file_untagged_parent, parent, false);
        return new ParentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ParentViewHolder holder, final int position) {
        final Release release;
        synchronized (mReleases) {
            release = mReleases.get(position);
        }

        holder.text.setText(release.getArtist_credit().get(0).getName() + " - " + release.getTitle());

        holder.adapter.setRelease(release);
        holder.adapter.notifyDataSetChanged();

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnReleaseClickListener != null) {
                    mOnReleaseClickListener.onReleaseClick(TaggerListAdapter.this, release);
                }
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnReleaseLongClickListener != null) {
                    mOnReleaseLongClickListener.onReleaseLongClick(TaggerListAdapter.this, release);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mReleases.size();
    }

    public List<Release> getReleases() {
        return new ArrayList<>(mReleases);
    }

    public void setOnTrackClickListener(OnTrackClickListener onTrackClickListener) {
        mOnTrackClickListener = onTrackClickListener;
    }

    public void setOnTrackLongClickListener(OnTrackLongClickListener onTrackLongClickListener) {
        mOnTrackLongClickListener = onTrackLongClickListener;
    }

    public void setOnReleaseClickListener(OnReleaseClickListener onReleaseClickListener) {
        mOnReleaseClickListener = onReleaseClickListener;
    }

    public void setOnReleaseLongClickListener(OnReleaseLongClickListener onReleaseLongClickListener) {
        mOnReleaseLongClickListener = onReleaseLongClickListener;
    }

    public void addRelease(Release release) {
        if (mReleases.contains(release)) {
            return;
        }

        mReleases.add(release);
        mNotifier.notifyItemChanged(mReleases.size() - 1);
    }

    public interface OnTrackClickListener {
        void onTrackClick(TaggerListAdapter adapter, Track track);
    }

    public interface OnTrackLongClickListener {
        void onTrackLongClick(TaggerListAdapter adapter, Track track);
    }

    public interface OnReleaseClickListener {
        void onReleaseClick(TaggerListAdapter adapter, Release release);
    }

    public interface OnReleaseLongClickListener {
        void onReleaseLongClick(TaggerListAdapter adapter, Release release);
    }

    public class ParentViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.root)
        View root;
        @Bind(R.id.text)
        TextView text;
        @Bind(R.id.recycler_view)
        RecyclerView recycler;
        @Bind(R.id.icon_collapse)
        View collapseButton;

        ChildAdapter adapter = new ChildAdapter();

        public ParentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            recycler.setAdapter(adapter);
            recycler.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            collapseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recycler.getVisibility() == View.VISIBLE) {
                        collapseButton.animate().rotation(180f);
                        ViewCompat.animate(recycler).alpha(0).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                recycler.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        collapseButton.animate().rotation(0f);
                        ViewCompat.animate(recycler).alpha(1).withStartAction(new Runnable() {
                            @Override
                            public void run() {
                                recycler.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        }
    }

    public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {
        List<Track> mTracks;

        public void setRelease(Release release) {
            LinkedList<Track> tracks = new LinkedList<>();

            for (Medium m : release.getMedia()) {
                tracks.addAll(m.getTracks());
            }

            mTracks = tracks;
        }

        @Override
        public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_untagged, parent, false);
            return new ChildViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ChildViewHolder holder, int position) {
            Track track = mTracks.get(position);
            holder.text.setText(track.getTitle());
        }

        @Override
        public int getItemCount() {
            return mTracks.size();
        }

        public class ChildViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.text)
            TextView text;

            public ChildViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }

    public static class Section {
        File file;
        String album;
        String artist;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Section section = (Section) o;

            if (file != null ? !file.equals(section.file) : section.file != null) return false;
            if (album != null ? !album.equals(section.album) : section.album != null) return false;
            return !(artist != null ? !artist.equals(section.artist) : section.artist != null);

        }

        @Override
        public int hashCode() {
            int result = file != null ? file.hashCode() : 0;
            result = 31 * result + (album != null ? album.hashCode() : 0);
            result = 31 * result + (artist != null ? artist.hashCode() : 0);
            return result;
        }
    }

    public static class Entry {
        File file;
        String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            return !(file != null ? !file.equals(entry.file) : entry.file != null);

        }

        @Override
        public int hashCode() {
            return file != null ? file.hashCode() : 0;
        }
    }
}
