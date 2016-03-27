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
    private final OrderedHashMap<Section, OrderedHashSet<Entry>> mFiles = new OrderedHashMap<>();
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
        final Map.Entry<Section, OrderedHashSet<Entry>> entry;
        synchronized (mFiles) {
            entry = mFiles.getEntryAt(position);
        }

        final Section section = entry.getKey();

        if (section.album != null && !section.album.isEmpty()) {
            if (section.artist != null && !section.artist.isEmpty()) {
                holder.text.setText(section.artist + " - " + section.album);
            } else {
                holder.text.setText(section.file.getName());
            }
        } else {
            holder.text.setText(section.file.getName());
        }

        holder.adapter.notifyDataSetChanged();
        holder.adapter.mChildFiles = entry.getValue();
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnSectionClickListener != null) {
                    mOnSectionClickListener.onSectionClick(UntaggedListAdapter.this, section);
                }
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnSectionLongClickListener != null) {
                    mOnSectionLongClickListener.onSectionLongClick(UntaggedListAdapter.this, section);
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

    public OrderedHashMap<Section, OrderedHashSet<Entry>> getFiles() {
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

                    Entry entry = new Entry();
                    entry.file = f;
                    Tag tag = audioFile.getTag();

                    if (tag != null) {
                        try {
                            entry.name = tag.getFirst(FieldKey.TITLE);
                        } catch (KeyNotFoundException ignored) {
                        }
                    }

                    if (entry.name == null || entry.name.isEmpty()) {
                        entry.name = entry.file.getName();
                    }

                    Section section = new Section();
                    section.file = dir;

                    try {
                        section.album = audioFile.getTag().getFirst(FieldKey.ALBUM);
                    } catch (KeyNotFoundException ignored) {
                    }

                    try {
                        section.artist = audioFile.getTag().getFirst(FieldKey.ARTIST);
                    } catch (KeyNotFoundException ignored) {
                    }


                    synchronized (mFiles) {
                        if (mFiles.containsKey(section)) {
                            final OrderedHashSet<Entry> dirSet = mFiles.get(section);
                            synchronized (dirSet) {
                                dirSet.add(entry);
                            }

                            final int position = mFiles.lastIndexOf(section);

                            Log.d(TAG, "Refreshed position: " + position);

                            mNotifier.notifyItemChanged(position);
                        } else {
                            OrderedHashSet<Entry> dirSet = new OrderedHashSet<>();
                            dirSet.add(entry);
                            mFiles.put(section, dirSet);

                            mNotifier.notifyItemInserted(mFiles.size() - 1);
                        }
                    }
                }
            }
        });
    }

    public interface OnItemClickListener {
        void onItemClick(UntaggedListAdapter adapter, Entry entry);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(UntaggedListAdapter adapter, Entry entry);
    }

    public interface OnSectionClickListener {
        void onSectionClick(UntaggedListAdapter adapter, Section section);
    }

    public interface OnSectionLongClickListener {
        void onSectionLongClick(UntaggedListAdapter adapter, Section section);
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
        OrderedHashSet<Entry> mChildFiles;

        public void setFiles(OrderedHashSet<Entry> files) {
            mChildFiles = files;
        }

        @Override
        public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_untagged, parent, false);
            return new ChildViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ChildViewHolder holder, int position) {
            Entry entry = mChildFiles.get(position);
            holder.text.setText(entry.name);
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
