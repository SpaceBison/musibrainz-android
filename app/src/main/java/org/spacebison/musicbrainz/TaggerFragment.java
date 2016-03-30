package org.spacebison.musicbrainz;

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

    public void onBrowserLookupResult(final String id) {
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

    public void saveTags() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<TaggerListAdapter.ReleaseTag> releaseTags = mAdapter.getReleaseTags();
                for (TaggerListAdapter.ReleaseTag rt : releaseTags) {
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

                    List<TaggerListAdapter.TrackTag> trackTags = rt.childAdapter.getTrackTags();
                    for (TaggerListAdapter.TrackTag tt : trackTags) {
                        try {
                            final AudioFile audioFile = AudioFileIO.read(tt.untaggedTrack.file);
                            final Tag tag = audioFile.getTagOrCreateDefault();
                            final Track track = tt.track;

                            List<Artist_credit> artistCredits = track.getArtist_credit();
                            if (artistCredits != null && !artistCredits.isEmpty()) {
                                Artist_credit artistCredit = artistCredits.get(0);
                                Artist artist = artistCredit.getArtist();
                                tag.setField(FieldKey.ARTIST, artist.getName());
                                tag.setField(FieldKey.ARTIST_SORT, artist.getSort_name());
                                tag.setField(FieldKey.MUSICBRAINZ_ARTISTID, artist.getId());
                            }

                            tag.setField(FieldKey.ALBUM, albumTitle);
                            tag.setField(FieldKey.ALBUM_ARTIST, albumArtistName);
                            tag.setField(FieldKey.ALBUM_ARTIST_SORT, albumArtistSort);

                            tag.setField(FieldKey.TITLE, track.getTitle());
                            tag.setField(FieldKey.TRACK, track.getNumber());
                            tag.setField(FieldKey.TRACK_TOTAL, Integer.toString(trackTags.size()));
                            tag.setField(FieldKey.MUSICBRAINZ_TRACK_ID, track.getId());

                            tag.setField(FieldKey.YEAR, release.getDate());
                            tag.setField(FieldKey.MUSICBRAINZ_RELEASE_COUNTRY, release.getCountry());
                            tag.setField(FieldKey.BARCODE, release.getBarcode().toString());
                            audioFile.setTag(tag);
                            audioFile.commit();
                            Log.d(TAG, "Saved tags for " + audioFile.getFile().getName());
                        } catch (CannotReadException | IOException | ReadOnlyFileException | InvalidAudioFrameException | TagException | CannotWriteException e) {
                            e.printStackTrace();
                        }
                    }
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
