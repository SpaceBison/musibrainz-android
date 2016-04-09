package org.spacebison.musicbrainz;

import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 09.03.16.
 */
public class UntaggedListAdapter extends RecyclerView.Adapter<UntaggedListAdapter.ParentViewHolder> {
    private static final String TAG = "UntaggedListAdapter";
    private final OrderedHashMap<UntaggedRelease, OrderedHashSet<UntaggedTrack>> mUntagged = new OrderedHashMap<>();
    public final RecyclerViewAdapterNotifier notifier = new RecyclerViewAdapterNotifier(this);
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;
    private OnSectionClickListener mOnSectionClickListener;
    private OnSectionLongClickListener mOnSectionLongClickListener;

    @Override
    public ParentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.item_file_untagged_parent, parent, false);
        return new ParentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ParentViewHolder holder, final int position) {
        final Map.Entry<UntaggedRelease, OrderedHashSet<UntaggedTrack>> entry;
        synchronized (mUntagged) {
            entry = mUntagged.getEntryAt(position);
        }

        final UntaggedRelease untaggedRelease = entry.getKey();
        holder.recycler.setAdapter(untaggedRelease.childAdapter);

        holder.text.setText(untaggedRelease.getName());

        int bgColorResId = untaggedRelease.marked ? R.color.colorAccentTransparent : R.color.transparent;
        holder.root.setBackgroundColor(ContextCompat.getColor(Musicbrainz.getAppContext(), bgColorResId));
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnSectionClickListener != null) {
                    mOnSectionClickListener.onSectionClick(UntaggedListAdapter.this, untaggedRelease);
                }
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnSectionLongClickListener != null) {
                    mOnSectionLongClickListener.onSectionLongClick(UntaggedListAdapter.this, untaggedRelease);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUntagged.size();
    }

    public OrderedHashMap<UntaggedRelease, OrderedHashSet<UntaggedTrack>> getUntagged() {
        return new OrderedHashMap<>(mUntagged);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    public void setOnSectionClickListener(OnSectionClickListener onSectionClickListener) {
        mOnSectionClickListener = onSectionClickListener;
    }

    public void setOnSectionLongClickListener(OnSectionLongClickListener onSectionLongClickListener) {
        mOnSectionLongClickListener = onSectionLongClickListener;
    }

    public synchronized void addFiles(final Collection<File> files) {
        Log.d(TAG, "Adding " + files.size() + " parent files");

        final Set<File> childFiles = new HashSet<File>();
        for (File f : files) {
            childFiles.addAll(Utils.getFilesRecursively(f));
        }

        Log.d(TAG, "Found " + childFiles.size() + " child files");

        for (File f : childFiles) {
            Log.d(TAG, "Adding file " + f.getName());
            File dir = f.getParentFile();
            AudioFile audioFile = null;
            try {
                audioFile = AudioFileIO.read(f);
            } catch (CannotReadException | IOException | ReadOnlyFileException | InvalidAudioFrameException | TagException e) {
                Log.w(TAG, "Error reading audio file: " + e);
            }

            if (audioFile == null) {
                continue;
            }

            UntaggedTrack untaggedTrack = new UntaggedTrack();
            untaggedTrack.file = f;
            Tag tag = audioFile.getTag();

            if (tag != null) {
                try {
                    untaggedTrack.title = tag.getFirst(FieldKey.TITLE);
                } catch (KeyNotFoundException ignored) {
                }
            }

            if (untaggedTrack.title == null || untaggedTrack.title.isEmpty()) {
                untaggedTrack.title = untaggedTrack.file.getName();
            }

            UntaggedRelease untaggedRelease = new UntaggedRelease();
            untaggedRelease.file = dir;

            try {
                untaggedRelease.album = audioFile.getTag().getFirst(FieldKey.ALBUM);
            } catch (KeyNotFoundException ignored) {
            }

            try {
                untaggedRelease.artist = audioFile.getTag().getFirst(FieldKey.ARTIST);
            } catch (KeyNotFoundException ignored) {
            }

            synchronized (mUntagged) {
                if (mUntagged.containsKey(untaggedRelease)) {
                    final OrderedHashSet<UntaggedTrack> dirSet = mUntagged.get(untaggedRelease);
                    dirSet.add(untaggedTrack);
                    final int position = mUntagged.lastIndexOf(untaggedRelease);
                    notifier.notifyItemChanged(position);
                } else {
                    final OrderedHashSet<UntaggedTrack> dirSet = new OrderedHashSet<>();
                    mUntagged.put(untaggedRelease, dirSet);
                    dirSet.add(untaggedTrack);
                    notifier.notifyItemInserted();
                }
            }
        }
    }

    public List<UntaggedTrack> getUntaggedTracks(UntaggedRelease untaggedRelease) {
        return mUntagged.get(untaggedRelease).toList();
    }

    public synchronized void setMarked(UntaggedRelease release, boolean marked) {
        release.marked = marked;
        notifier.notifyItemChanged(mUntagged.indexOf(release));
    }

    public synchronized void setMarked(final UntaggedTrack track, boolean marked) {
        track.marked = marked;

        for (Map.Entry<UntaggedRelease, OrderedHashSet<UntaggedTrack>> entry : mUntagged.entrySet()) {
            final OrderedHashSet<UntaggedTrack> untaggedTracks = entry.getValue();
            if (untaggedTracks.contains(track)) {
                entry.getKey().childAdapter.notifier.notifyItemChanged(untaggedTracks.indexOf(track));
                break;
            }
        }
    }

    public synchronized void removeUntaggedRelease(UntaggedRelease untaggedRelease) {
        final int position = mUntagged.indexOf(untaggedRelease);
        mUntagged.remove(untaggedRelease);
        notifier.notifyItemRemoved(position);
    }

    public synchronized void removeUntaggedTrack(final UntaggedTrack untaggedTrack) {
        for (Map.Entry<UntaggedRelease, OrderedHashSet<UntaggedTrack>> entry : mUntagged.entrySet()) {
            final OrderedHashSet<UntaggedTrack> untaggedTracks = entry.getValue();
            if (untaggedTracks.contains(untaggedTrack)) {
                final int position = untaggedTracks.indexOf(untaggedTrack);
                untaggedTracks.remove(position);
                entry.getKey().childAdapter.notifier.notifyItemRemoved(position);
                break;
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(UntaggedListAdapter adapter, UntaggedTrack untaggedTrack);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(UntaggedListAdapter adapter, UntaggedTrack untaggedTrack);
    }

    public interface OnSectionClickListener {
        void onSectionClick(UntaggedListAdapter adapter, UntaggedRelease untaggedRelease);
    }

    public interface OnSectionLongClickListener {
        void onSectionLongClick(UntaggedListAdapter adapter, UntaggedRelease untaggedRelease);
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

        public ParentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            recycler.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recycler.setNestedScrollingEnabled(false);
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
        private final UntaggedRelease mUntaggedRelease;
        public final RecyclerViewAdapterNotifier notifier;

        public ChildAdapter(UntaggedRelease untaggedRelease) {
            mUntaggedRelease = untaggedRelease;
            notifier = new RecyclerViewAdapterNotifier(this);
        }

        @Override
        public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_untagged, parent, false);
            return new ChildViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ChildViewHolder holder, int position) {
            final OrderedHashSet<UntaggedTrack> tracks = getUntaggedTracks();
            final UntaggedTrack untaggedTrack = tracks.get(position);
            holder.text.setText(untaggedTrack.title);
            int bgColorResId = untaggedTrack.marked ? R.color.colorAccent : R.color.transparent;
            holder.root.setBackgroundColor(ContextCompat.getColor(Musicbrainz.getAppContext(), bgColorResId));
            holder.root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(UntaggedListAdapter.this, untaggedTrack);
                    }
                }
            });
            holder.root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemLongClickListener != null) {
                        mOnItemLongClickListener.onItemLongClick(UntaggedListAdapter.this, untaggedTrack);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            final OrderedHashSet tracks = getUntaggedTracks();
            return tracks == null ? 0 : tracks.size();
        }

        public OrderedHashSet<UntaggedTrack> getUntaggedTracks() {
            return mUntagged.get(mUntaggedRelease);
        }

        public void refresh(int position) {
            getUntaggedTracks().get(position).updateName();
            notifier.notifyItemChanged(position);
        }

        public void refreshAll() {
            final OrderedHashSet<UntaggedTrack> untaggedTracks = getUntaggedTracks();
            final int size = untaggedTracks.size();
            for (int i = 0; i < size; ++i) {
                untaggedTracks.get(i).updateName();
                notifier.notifyItemChanged(i);
            }
        }

        public class ChildViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.root)
            ViewGroup root;
            @Bind(R.id.text)
            TextView text;

            public ChildViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }

    public class UntaggedRelease {
        File file;
        String album;
        String artist;
        boolean marked = false;
        ChildAdapter childAdapter = new ChildAdapter(this);

        public String getName() {
            if (album != null && !album.isEmpty() &&
                    artist != null && !artist.isEmpty()) {
                return artist + " - " + album;
            } else {
                return file.getName();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UntaggedRelease that = (UntaggedRelease) o;

            return !(file != null ? !file.equals(that.file) : that.file != null);

        }

        @Override
        public int hashCode() {
            return file != null ? file.hashCode() : 0;
        }
    }

    public static class UntaggedTrack {
        File file;
        String title;
        String album;
        String artist;
        int track;
        boolean marked;

        public String updateName() {
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();

                if(tag != null) {
                    title = tag.getFirst(FieldKey.TITLE);
                    album = tag.getFirst(FieldKey.ALBUM);
                    artist = tag.getFirst(FieldKey.ARTIST);
                    track = Integer.parseInt(tag.getFirst(FieldKey.TRACK));
                }
            } catch (TagException | KeyNotFoundException | ReadOnlyFileException | InvalidAudioFrameException | IOException | CannotReadException e) {
                e.printStackTrace();
            }
            return title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UntaggedTrack untaggedTrack = (UntaggedTrack) o;

            return !(file != null ? !file.equals(untaggedTrack.file) : untaggedTrack.file != null);

        }

        @Override
        public int hashCode() {
            return file != null ? file.hashCode() : 0;
        }
    }
}
