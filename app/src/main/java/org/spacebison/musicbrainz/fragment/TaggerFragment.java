package org.spacebison.musicbrainz.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.spacebison.musicbrainz.Api;
import org.spacebison.musicbrainz.Musicbrainz;
import org.spacebison.musicbrainz.R;
import org.spacebison.musicbrainz.util.Utils;
import org.spacebison.musicbrainz.adapter.TaggerListAdapter;
import org.spacebison.musicbrainz.adapter.UntaggedListAdapter;
import org.spacebison.musicbrainz.api.Artist;
import org.spacebison.musicbrainz.api.Artist_credit;
import org.spacebison.musicbrainz.api.Release;
import org.spacebison.musicbrainz.api.Track;

import java.io.IOException;
import java.util.List;
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

    public void saveTags(final ProgressListener listener) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<TaggerListAdapter.ReleaseTag> releaseTags = mAdapter.getReleaseTags();
                int maxProgress = 0;
                int progress = 0;

                if (listener != null) {
                    listener.onStarted();

                    for (final TaggerListAdapter.ReleaseTag rt : releaseTags) {
                        maxProgress += rt.childAdapter.getItemCount();
                    }
                }

                for (final TaggerListAdapter.ReleaseTag rt : releaseTags) {
                    Release release = rt.release;
                    String albumTitle = release.getTitle();
                    String albumArtistName = null;
                    String albumArtistSort = null;
                    List<Artist_credit> albumArtistCredits = release.getArtist_credit();

                    if (albumArtistCredits != null && !albumArtistCredits.isEmpty()) {
                        final Artist albumArtist = albumArtistCredits.get(0).getArtist();
                        albumArtistName = albumArtist.getName();
                        albumArtistSort = albumArtist.getSort_name();
                    }

                    final TaggerListAdapter.ChildAdapter childAdapter = rt.childAdapter;
                    final List<TaggerListAdapter.TrackTag> trackTags = childAdapter.getTrackTags();
                    final int size = trackTags.size();
                    for (int i = 0; i < size; ++i) {
                        final TaggerListAdapter.TrackTag tt = trackTags.get(i);
                        final UntaggedListAdapter.UntaggedTrack untaggedTrack = tt.untaggedTrack;

                        if (untaggedTrack == null) {
                            continue;
                        }

                        try {
                            final AudioFile audioFile = AudioFileIO.read(untaggedTrack.file);
                            final Tag tag = audioFile.getTagOrCreateDefault();
                            final Track track = tt.track;

                            List<Artist_credit> artistCredits = track.getArtist_credit();
                            if (artistCredits != null && !artistCredits.isEmpty()) {
                                Artist_credit artistCredit = artistCredits.get(0);
                                Artist artist = artistCredit.getArtist();

                                final String name = artist.getName();
                                if (name != null) {
                                    tag.setField(FieldKey.ARTIST, name);
                                }

                                final String sortName = artist.getSort_name();
                                if (sortName != null) {
                                    tag.setField(FieldKey.ARTIST_SORT, sortName);
                                }

                                final String id = artist.getId();
                                if (id != null) {
                                    tag.setField(FieldKey.MUSICBRAINZ_ARTISTID, id);
                                }
                            }

                            if (albumTitle != null) {
                                tag.setField(FieldKey.ALBUM, albumTitle);
                            }

                            if (albumArtistName != null) {
                                tag.setField(FieldKey.ALBUM_ARTIST, albumArtistName);
                            }

                            if (albumArtistSort != null) {
                                tag.setField(FieldKey.ALBUM_ARTIST_SORT, albumArtistSort);
                            }

                            final String title = track.getTitle();
                            if (title != null) {
                                tag.setField(FieldKey.TITLE, title);
                            }

                            final String number = track.getNumber();
                            if (number != null) {
                                tag.setField(FieldKey.TRACK, number);
                            }

                            final String trackTotal = Integer.toString(trackTags.size());
                            if (trackTotal != null) {
                                tag.setField(FieldKey.TRACK_TOTAL, trackTotal);
                            }

                            final String id = track.getId();
                            if (id != null) {
                                tag.setField(FieldKey.MUSICBRAINZ_TRACK_ID, id);
                            }

                            final String date = release.getDate();
                            if (date != null) {
                                tag.setField(FieldKey.YEAR, date);
                            }

                            final String country = release.getCountry();
                            if (country != null) {
                                tag.setField(FieldKey.MUSICBRAINZ_RELEASE_COUNTRY, country);
                            }

                            final String barcode = release.getBarcode();
                            if (barcode != null) {
                                tag.setField(FieldKey.BARCODE, barcode);
                            }

                            audioFile.setTag(tag);
                            audioFile.commit();

                            final int position = i;
                            mExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    untaggedTrack.updateName();
                                    tt.tagged = true;
                                    childAdapter.notifier.notifyItemChanged(position);
                                }
                            });

                            if (listener != null) {
                                listener.onProgressChanged(maxProgress, ++progress);
                            }

                            Log.d(TAG, "Saved tags for " + audioFile.getFile().getName());
                        } catch (CannotReadException | IOException | ReadOnlyFileException | InvalidAudioFrameException | TagException | CannotWriteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (listener != null) {
                    listener.onFinished();
                }

                return null;
            }
        }.execute();
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
