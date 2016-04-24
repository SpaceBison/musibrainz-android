package org.spacebison.musicbrainz.adapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import org.spacebison.musicbrainz.Musicbrainz;
import org.spacebison.musicbrainz.R;
import org.spacebison.musicbrainz.adapter.UntaggedListAdapter.UntaggedRelease;
import org.spacebison.musicbrainz.adapter.UntaggedListAdapter.UntaggedTrack;
import org.spacebison.musicbrainz.api.Medium;
import org.spacebison.musicbrainz.api.Release;
import org.spacebison.musicbrainz.api.Track;
import org.spacebison.musicbrainz.collection.OrderedHashSet;
import org.spacebison.musicbrainz.service.TaggerService;
import org.spacebison.musicbrainz.util.RecyclerViewAdapterNotifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import info.debatty.java.stringsimilarity.Levenshtein;

/**
 * Created by cmb on 09.03.16.
 */
public class TaggerListAdapter extends RecyclerView.Adapter<TaggerListAdapter.ParentViewHolder> {
    private static final String TAG = "TaggerListAdapter";
    public final RecyclerViewAdapterNotifier notifier = new RecyclerViewAdapterNotifier(this);
    private final OrderedHashSet<ReleaseTag> mReleaseTags = new OrderedHashSet<>();
    private OnTrackTagClickListener mOnTrackTagClickListener;
    private OnTrackTagLongClickListener mOnTrackTagLongClickListener;
    private OnReleaseTagClickListener mOnReleaseTagClickListener;
    private OnReleaseTagLongClickListener mOnReleaseTagLongClickListener;
    private Receiver mReceiver = new Receiver();

    @Override
    public ParentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.item_file_untagged_parent, parent, false);
        return new ParentViewHolder(v);
    }

    public List<ReleaseTag> getReleaseTags() {
        return mReleaseTags.toList();
    }
public void setOnTrackTagClickListener(OnTrackTagClickListener onTrackTagClickListener) {
        mOnTrackTagClickListener = onTrackTagClickListener;
    }@Override
    public void onBindViewHolder(ParentViewHolder holder, final int position) {
        final ReleaseTag releaseTag;
        synchronized (mReleaseTags) {
            releaseTag = mReleaseTags.get(position);
        }

        holder.recycler.setAdapter(releaseTag.childAdapter);

        holder.text.setText(releaseTag.release.getArtist_credit().get(0).getName() + " - " + releaseTag.release.getTitle());

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "klik1");
                if (mOnReleaseTagClickListener != null) {
                    mOnReleaseTagClickListener.onReleaseTagClick(TaggerListAdapter.this, releaseTag);
                }
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnReleaseTagLongClickListener != null) {
                    mOnReleaseTagLongClickListener.onReleaseTagLongClick(TaggerListAdapter.this, releaseTag);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

        public void setOnTrackTagLongClickListener(OnTrackTagLongClickListener onTrackTagLongClickListener) {
        mOnTrackTagLongClickListener = onTrackTagLongClickListener;
    }
    public void setOnReleaseTagClickListener(OnReleaseTagClickListener onReleaseTagClickListener) {
        mOnReleaseTagClickListener = onReleaseTagClickListener;
    }

    @Override
    public int getItemCount() {
        return mReleaseTags.size();
    }

    public void setOnReleaseTagLongClickListener(OnReleaseTagLongClickListener onReleaseTagLongClickListener) {
        mOnReleaseTagLongClickListener = onReleaseTagLongClickListener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        Musicbrainz.getAppContext().registerReceiver(mReceiver, new IntentFilter(TaggerService.ACTION_UPDATE_TRACK_TAG));
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        Musicbrainz.getAppContext().unregisterReceiver(mReceiver);
    }

    public synchronized void addRelease(Release release) {
        if (release == null) {
            return;
        }

        ReleaseTag releaseTag = new ReleaseTag(release, new ChildAdapter());

        if (mReleaseTags.contains(releaseTag)) {
            return;
        }

        mReleaseTags.add(releaseTag);
        notifier.notifyItemInserted();
    }

    public synchronized void removeRelease(Release release) {
        if (release == null) {
            return;
        }

        ReleaseTag releaseTag = new ReleaseTag(release, null);
        int index = mReleaseTags.indexOf(releaseTag);

        if (index >= 0) {
            mReleaseTags.remove(index);
            notifier.notifyItemRemoved(index);
        }
    }

    public synchronized void setUntaggedRelease(ReleaseTag releaseTag, UntaggedRelease untaggedRelease) {
        releaseTag.untagged = untaggedRelease;

        final ChildAdapter childAdapter = releaseTag.childAdapter;
        final ArrayList<TrackTag> trackTags = new ArrayList<>(childAdapter.mTrackTags);
        final OrderedHashSet<UntaggedTrack> untaggedTracks = untaggedRelease.childAdapter.getUntaggedTracks();

        final Levenshtein levenshtein = new Levenshtein();

        for (Iterator<TrackTag> it = trackTags.iterator(); it.hasNext();) {
            TrackTag tt = it.next();

            if (tt.untaggedTrack != null) {
                it.remove();
            }
        }

        final int size = untaggedTracks.size();
        for (int i = 0; i < size; ++i) {
            UntaggedTrack ut = untaggedTracks.get(i);
            double bestScore = Double.POSITIVE_INFINITY;
            TrackTag bestTrack = null;

            for (TrackTag tt : trackTags) {
                double score = levenshtein.distance(ut.title, tt.track.getTitle());

                if (score < bestScore) {
                    bestScore = score;
                    bestTrack = tt;
                }
            }

            if (bestTrack != null) {
                bestTrack.untaggedTrack = ut;
                trackTags.remove(bestTrack);
                childAdapter.notifier.notifyItemChanged(i);
            }
        }

        childAdapter.notifier.notifyDataSetChanged();
        notifier.notifyItemChanged(mReleaseTags.indexOf(releaseTag));
    }

    public synchronized void setUntaggedTrack(ReleaseTag releaseTag, TrackTag trackTag, UntaggedTrack untaggedTrack) {
        trackTag.untaggedTrack = untaggedTrack;
        releaseTag.childAdapter.notifier.notifyItemChanged(releaseTag.childAdapter.mTrackTags.indexOf(trackTag));
    }

    public void removeTrackTag(TrackTag trackTag) {
        for (ReleaseTag releaseTag : mReleaseTags) {
            final ChildAdapter childAdapter = releaseTag.childAdapter;
            final List<TrackTag> trackTags = childAdapter.getTrackTags();
            int index = trackTags.indexOf(trackTag);

            if (index != -1) {
                trackTags.remove(index);
                childAdapter.notifier.notifyItemRemoved(index);
            }
        }
    }

    public void removeReleaseTag(ReleaseTag releaseTag) {
        final int index = mReleaseTags.indexOf(releaseTag);

        if(index != -1) {
            mReleaseTags.remove(index);
            notifier.notifyItemRemoved(index);
        }
    }

    public interface OnTrackTagClickListener {
        void onTrackTagClick(TaggerListAdapter adapter, ReleaseTag releaseTag, TrackTag track);
    }

    public interface OnTrackTagLongClickListener {
        void onTrackTagLongClick(TaggerListAdapter adapter, ReleaseTag releaseTag, TrackTag track);
    }

    public interface OnReleaseTagClickListener {
        void onReleaseTagClick(TaggerListAdapter adapter, ReleaseTag release);
    }

    public interface OnReleaseTagLongClickListener {
        void onReleaseTagLongClick(TaggerListAdapter adapter, ReleaseTag release);
    }

    public static class TrackTag implements Serializable {
        public Track track;
        public UntaggedTrack untaggedTrack;
        public State state = State.UNTAGGED;

        public TrackTag(Track track) {
            this.track = track;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TrackTag trackTag = (TrackTag) o;

            return !(track != null ? !track.equals(trackTag.track) : trackTag.track != null);
        }

        @Override
        public String toString() {
            return "TrackTag{" +
                    "track=" + track +
                    ", untaggedTrack=" + untaggedTrack +
                    ", state=" + state +
                    '}';
        }

        public enum State {
            UNTAGGED,
            TAGGING,
            TAGGED;
        }

        @Override
        public int hashCode() {
            return track != null ? track.hashCode() : 0;
        }
    }

    public static class ReleaseTag implements Serializable {
        public transient final ChildAdapter childAdapter;
        public List<TrackTag> trackTags;
        public UntaggedRelease untagged;
        public Release release;

        public ReleaseTag(@NonNull Release release, @Nullable ChildAdapter adapter) {
            this.release = release;
            childAdapter = adapter;

            if (childAdapter != null) {
                childAdapter.setReleaseTag(this);
                trackTags = childAdapter.mTrackTags;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReleaseTag releaseTag = (ReleaseTag) o;

            return !(release != null ? !release.equals(releaseTag.release) : releaseTag.release != null);

        }

        @Override
        public int hashCode() {
            return release != null ? release.hashCode() : 0;
        }
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
        public final RecyclerViewAdapterNotifier notifier = new RecyclerViewAdapterNotifier(this);
        private ReleaseTag mReleaseTag;
        private List<TrackTag> mTrackTags;

        public void setReleaseTag(ReleaseTag releaseTag) {
            mReleaseTag = releaseTag;

            if (mReleaseTag == null) {
                return;
            }

            int size = 0;

            if (mTrackTags != null) {
                size = mTrackTags.size();
            }

            mTrackTags = new LinkedList<>();

            if (size > 0) {
                notifier.notifyItemRangeRemoved(0, size);
            }

            for (Medium m : mReleaseTag.release.getMedia()) {
                List<Track> tracks = m.getTracks();
                for (Track t : tracks) {
                    mTrackTags.add(new TrackTag(t));
                    notifier.notifyItemInserted(mTrackTags.size() - 1);
                }
            }
        }

        public List<TrackTag> getTrackTags() {
            return mTrackTags;
        }

        public class ChildViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.root)
            ViewGroup root;
            @Bind(R.id.text)
            TextView text;
            @Bind(R.id.text2)
            TextView text2;
            @Bind(R.id.tagged_indicator)
            ImageView taggedIndicator;

            public ChildViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }

        @Override
        public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_tagger, parent, false);
            return new ChildViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ChildViewHolder holder, int position) {
            final TrackTag trackTag = mTrackTags.get(position);
            holder.text.setText(trackTag.track.getTitle());

            if (trackTag.untaggedTrack != null) {
                holder.text2.setText(trackTag.untaggedTrack.title);
                holder.text2.setVisibility(View.VISIBLE);
            } else {
                holder.text2.setVisibility(View.GONE);
            }

            switch (trackTag.state) {
                case UNTAGGED:
                    //holder.taggedIndicator.animate().alpha(0);
                    holder.taggedIndicator.setVisibility(View.INVISIBLE);
                    break;
                case TAGGING:
                    holder.taggedIndicator.setImageResource(R.drawable.ic_settings_24dp);
                    //holder.taggedIndicator.animate().alpha(1);
                    holder.taggedIndicator.setVisibility(View.VISIBLE);
                    final RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotateAnimation.setDuration(1000);
                    rotateAnimation.setRepeatCount(Animation.INFINITE);
                    rotateAnimation.setInterpolator(new LinearInterpolator());
                    holder.taggedIndicator.startAnimation(rotateAnimation);
                    break;
                case TAGGED:
                    holder.taggedIndicator.clearAnimation();
                    holder.taggedIndicator.setVisibility(View.VISIBLE);
                    holder.taggedIndicator.setImageResource(R.drawable.ic_done_24dp);
                    break;
            }

            holder.root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnTrackTagClickListener != null) {
                        mOnTrackTagClickListener.onTrackTagClick(TaggerListAdapter.this, mReleaseTag, trackTag);
                    }
                }
            });

            holder.root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnTrackTagLongClickListener != null) {
                        mOnTrackTagLongClickListener.onTrackTagLongClick(TaggerListAdapter.this, mReleaseTag, trackTag);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mTrackTags.size();
        }


    }

    public class Receiver extends BroadcastReceiver {

        public Receiver() {}

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got broadcast: " + intent);
            switch (intent.getAction()) {
                case TaggerService.ACTION_UPDATE_TRACK_TAG:
                    TrackTag trackTag = (TrackTag) intent.getSerializableExtra(TaggerService.EXTRA_TRACK_TAG);
                    TrackTag.State state = (TrackTag.State) intent.getSerializableExtra(TaggerService.EXTRA_TRACK_TAG_STATE);

                    for (int i = 0; i < mReleaseTags.size(); ++i) {
                        final ReleaseTag releaseTag = mReleaseTags.get(i);
                        final List<TrackTag> trackTags = releaseTag.childAdapter.getTrackTags();
                        for (int j = 0; j < trackTags.size(); ++j) {
                            TrackTag tt = trackTags.get(j);
                            if (tt.equals(trackTag)) {
                                Log.d(TAG, "New state: " + state);
                                tt.state = state;
                                tt.untaggedTrack.updateName();
                                releaseTag.childAdapter.notifier.notifyItemChanged(j);
                                //notifier.notifyItemChanged(i);
                            }
                        }
                    }
            }
        }
    }
}
