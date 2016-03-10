package org.spacebison.musicbrainz;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 09.03.16.
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private final MappedHashSet<File> mFiles = new MappedHashSet<>();
    private AdapterView.OnItemClickListener mOnItemClickListener;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.item_file_chooser, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final File file = mFiles.get(position);
        holder.primaryText.setText(file.getName());
        holder.secondaryText.setText(file.getParent());
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(null, v, position, file.hashCode());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public MappedHashSet<File> getFiles() {
        return mFiles;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.root)
        View root;
        @Bind(R.id.text_primary)
        TextView primaryText;
        @Bind(R.id.text_secondary)
        TextView secondaryText;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
