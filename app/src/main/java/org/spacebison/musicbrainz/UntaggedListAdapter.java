package org.spacebison.musicbrainz;

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
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 09.03.16.
 */
public class UntaggedListAdapter extends RecyclerView.Adapter<UntaggedListAdapter.ParentViewHolder> {
    private static final String TAG = "UntaggedListAdapter";
    private final OrderedHashMap<UntaggedRelease, OrderedHashSet<UntaggedTrack>> mFiles = new OrderedHashMap<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final LinkedList<ChildAdapter> mChildAdapters = new LinkedList<>();
    private final RecyclerViewAdapterNotifier mNotifier = new RecyclerViewAdapterNotifier(this);
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
        synchronized (mFiles) {
            entry = mFiles.getEntryAt(position);
        }

        final UntaggedRelease untaggedRelease = entry.getKey();

        if (untaggedRelease.album != null && !untaggedRelease.album.isEmpty()) {
            if (untaggedRelease.artist != null && !untaggedRelease.artist.isEmpty()) {
                holder.text.setText(untaggedRelease.artist + " - " + untaggedRelease.album);
            } else {
                holder.text.setText(untaggedRelease.file.getName());
            }
        } else {
            holder.text.setText(untaggedRelease.file.getName());
        }

        holder.adapter.notifyDataSetChanged();
        holder.adapter.mChildFiles = entry.getValue();
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
        return mFiles.size();
    }

    public OrderedHashMap<UntaggedRelease, OrderedHashSet<UntaggedTrack>> getFiles() {
        return new OrderedHashMap<>(mFiles);
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

    public void addFiles(final Collection<File> files) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
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
                            untaggedTrack.name = tag.getFirst(FieldKey.TITLE);
                        } catch (KeyNotFoundException ignored) {
                        }
                    }

                    if (untaggedTrack.name == null || untaggedTrack.name.isEmpty()) {
                        untaggedTrack.name = untaggedTrack.file.getName();
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


                    synchronized (mFiles) {
                        if (mFiles.containsKey(untaggedRelease)) {
                            final OrderedHashSet<UntaggedTrack> dirSet = mFiles.get(untaggedRelease);
                            synchronized (dirSet) {
                                dirSet.add(untaggedTrack);
                            }

                            final int position = mFiles.lastIndexOf(untaggedRelease);

                            Log.d(TAG, "Refreshed position: " + position);

                            mNotifier.notifyItemChanged(position);
                        } else {
                            OrderedHashSet<UntaggedTrack> dirSet = new OrderedHashSet<>();
                            dirSet.add(untaggedTrack);
                            mFiles.put(untaggedRelease, dirSet);

                            mNotifier.notifyItemInserted(mFiles.size() - 1);
                        }
                    }
                }
            }
        });
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
        OrderedHashSet<UntaggedTrack> mChildFiles;

        public void setFiles(OrderedHashSet<UntaggedTrack> files) {
            mChildFiles = files;
        }

        @Override
        public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_untagged, parent, false);
            return new ChildViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ChildViewHolder holder, int position) {
            UntaggedTrack untaggedTrack = mChildFiles.get(position);
            holder.text.setText(untaggedTrack.name);
        }

        @Override
        public int getItemCount() {
            return mChildFiles.size();
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

    public static class UntaggedRelease {
        File file;
        String album;
        String artist;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UntaggedRelease untaggedRelease = (UntaggedRelease) o;

            if (file != null ? !file.equals(untaggedRelease.file) : untaggedRelease.file != null) return false;
            if (album != null ? !album.equals(untaggedRelease.album) : untaggedRelease.album != null) return false;
            return !(artist != null ? !artist.equals(untaggedRelease.artist) : untaggedRelease.artist != null);

        }

        @Override
        public int hashCode() {
            int result = file != null ? file.hashCode() : 0;
            result = 31 * result + (album != null ? album.hashCode() : 0);
            result = 31 * result + (artist != null ? artist.hashCode() : 0);
            return result;
        }
    }

    public static class UntaggedTrack {
        File file;
        String name;

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
