package org.spacebison.musicbrainz;

import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 10.03.16.
 */
public class FilePickerAdapter extends RecyclerView.Adapter<FilePickerAdapter.ViewHolder> {
    public static final String MIME_PREFIX_AUDIO = "audio/";
    private static final String TAG = "FilePickerAdapter";
    private static final FileComparator FILE_COMPARATOR = new FileComparator();
    private final HashSet<File> mCheckedFiles = new HashSet<>();
    private File mCurrentDir;
    private File[] mFiles;
    private Listener mListener;

    public FilePickerAdapter(File currentDir) {
        if (currentDir != null && currentDir.isDirectory()) {
            setDir(currentDir);
        } else {
            setDir(Environment.getRootDirectory());
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_picker, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final File file = mFiles[position];
        holder.text.setText(file.getName());
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(mCheckedFiles.contains(file));
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mCheckedFiles.add(file);
                } else {
                    mCheckedFiles.remove(file);
                }
            }
        });

        if (file.isDirectory()) {
            holder.icon.setImageResource(R.drawable.ic_folder_24dp);
            holder.root.setOnClickListener(new OnDirClickListener(holder, file));
        } else {
            holder.icon.setImageResource(getIcon(file));
            holder.root.setOnClickListener(new OnFileClickListener(holder));
        }
    }

    @Override
    public int getItemCount() {
        return mFiles.length;
    }

    public void goUp() {
        File parent = mCurrentDir.getParentFile();
        if (parent != null) {
            setDir(parent);
        }
    }

    public Set<File> getCheckedFiles() {
        return Collections.unmodifiableSet(mCheckedFiles);
    }

    public File getDir() {
        return mCurrentDir;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url.replace(' ', '_'));
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static int getIcon(File file) {
        final String mimeType = getMimeType(file.getName());
        if (mimeType != null && mimeType.startsWith(MIME_PREFIX_AUDIO)) {
            return R.drawable.ic_audiotrack_24dp;
        } else {
            return R.drawable.ic_insert_drive_file_24dp;
        }
    }

    private void setDir(File dir) {
        mCurrentDir = dir;
        mFiles = mCurrentDir.listFiles();
        Arrays.sort(mFiles, FILE_COMPARATOR);
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onDirChanged(dir);
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.root)
        public View root;
        @Bind(R.id.text)
        public TextView text;
        @Bind(R.id.ic_file)
        public ImageView icon;
        @Bind(R.id.check)
        public CheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface Listener {
        void onDirChanged(File dir);
    }

    private class OnDirClickListener implements View.OnClickListener {
        private ViewHolder mHolder;
        private File mDir;

        public OnDirClickListener(ViewHolder holder, File dir) {
            mHolder = holder;
            mDir = dir;
        }

        @Override
        public void onClick(View v) {
            setDir(mDir);
        }
    }

    private class OnFileClickListener implements View.OnClickListener {
        private ViewHolder mHolder;

        public OnFileClickListener(ViewHolder holder) {
            mHolder = holder;
        }

        @Override
        public void onClick(View v) {
            mHolder.checkBox.setChecked(!mHolder.checkBox.isChecked());
        }
    }

    private static class FileComparator implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            if (lhs.isDirectory()) {
                if (rhs.isDirectory()) {
                    return lhs.getName().compareTo(rhs.getName());
                } else {
                    return -1;
                }
            } else {
                if (rhs.isDirectory()) {
                    return 1;
                } else {
                    return lhs.getName().compareTo(rhs.getName());
                }
            }
        }
    }
}
