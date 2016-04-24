package org.spacebison.musicbrainz.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.spacebison.musicbrainz.R;
import org.spacebison.musicbrainz.adapter.TaggerListAdapter;
import org.spacebison.musicbrainz.adapter.UntaggedListAdapter;
import org.spacebison.musicbrainz.api.Artist;
import org.spacebison.musicbrainz.api.Artist_credit;
import org.spacebison.musicbrainz.api.Release;
import org.spacebison.musicbrainz.api.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cmb on 23.04.16.
 */
public class TaggerService extends IntentService {
    public static final String ACTION_UPDATE_TRACK_TAG = "org.spacebison.musicbrainz.ACTION_UPDATE_TRACK_TAG";
    public static final String EXTRA_TRACK_TAG = "org.spacebison.musicbrainz.EXTRA_TRACK_TAG";
    public static final String EXTRA_TRACK_TAG_STATE = "org.spacebison.musicbrainz.EXTRA_TRACK_TAG_STATE";
    private static final String TAG = "TaggerService";
    private static final String ACTION_SAVE_TAGS = "saveTags";
    private static final String EXTRA_RELEASE_TAGS = "realeaseTags";
    public static final int NOTIF_ID = 12345;

    public TaggerService() {
        super(TAG + "Thread");
    }

    public static void saveTags(Context context, List<TaggerListAdapter.ReleaseTag> releaseTags) {
        Intent intent = new Intent(context, TaggerService.class);
        intent.setAction(ACTION_SAVE_TAGS);
        intent.putExtra(EXTRA_RELEASE_TAGS, new ArrayList<>(releaseTags));
        final ComponentName componentName = context.startService(intent);
        Log.d(TAG, "Started: " + componentName);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Intent: " + intent);

        final String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_SAVE_TAGS:
                saveTagsInternal((List<TaggerListAdapter.ReleaseTag>) intent.getSerializableExtra(EXTRA_RELEASE_TAGS));
                return;

            default:
                Log.w(TAG, "Unsupported action: " + action);
        }
    }

    private void saveTagsInternal(List<TaggerListAdapter.ReleaseTag> releaseTags) {
        int progress = 0;
        int maxProgress = 0;

        for (final TaggerListAdapter.ReleaseTag rt : releaseTags) {
            maxProgress += rt.trackTags.size();
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Writing tags")
                .setSmallIcon(R.drawable.ic_label_white_24dp)
                .setProgress(0, maxProgress, false);

        startForeground(NOTIF_ID, builder.build());

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

            final int size = rt.trackTags.size();
            for (int i = 0; i < size; ++i) {
                Log.d(TAG, "Progress: " + progress + '/' + maxProgress);
                builder.setProgress(++progress, maxProgress, false);
                startForeground(NOTIF_ID, builder.build());

                final TaggerListAdapter.TrackTag tt = rt.trackTags.get(i);
                final UntaggedListAdapter.UntaggedTrack untaggedTrack = tt.untaggedTrack;

                if (untaggedTrack == null) {
                    continue;
                }

                final Intent taggingIntent = new Intent(ACTION_UPDATE_TRACK_TAG);
                taggingIntent.putExtra(EXTRA_TRACK_TAG, tt);
                taggingIntent.putExtra(EXTRA_TRACK_TAG_STATE, TaggerListAdapter.TrackTag.State.TAGGING);
                sendBroadcast(taggingIntent);

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

                    final String trackTotal = Integer.toString(rt.trackTags.size());
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

                    final Intent taggedIntent = new Intent(ACTION_UPDATE_TRACK_TAG);
                    taggedIntent.putExtra(EXTRA_TRACK_TAG, tt);
                    taggedIntent.putExtra(EXTRA_TRACK_TAG_STATE, TaggerListAdapter.TrackTag.State.TAGGED);
                    sendBroadcast(taggedIntent);

                    Log.d(TAG, "Saved tags for " + audioFile.getFile().getName());
                } catch (CannotReadException | IOException | ReadOnlyFileException | InvalidAudioFrameException | TagException | CannotWriteException e) {
                    e.printStackTrace();
                }
            }
        }

        stopForeground(true);
    }
}
