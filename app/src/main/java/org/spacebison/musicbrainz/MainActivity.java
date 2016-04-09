package org.spacebison.musicbrainz;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.chromium.customtabsclient.shared.CustomTabsHelper;
import org.chromium.customtabsclient.shared.ServiceConnection;
import org.chromium.customtabsclient.shared.ServiceConnectionCallback;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.Bind;
import butterknife.ButterKnife;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener, ServiceConnectionCallback {
    private static final String TAG = "MainActivity";
    private static final int CHOOSE_FILE_REQUEST_CODE = 1;
    public static final String MUSICBRAINZ_QUERY_SCHEME = "http";
    public static final String MUSICBRAINZ_DOMAIN = "musicbrainz.org";
    public static final String MUSICBRAINZ_TAG_LOOKUP = "taglookup";
    public static final String MUSICBRAINZ_PARAM_PORT = "tport";
    public static final String MUSICBRAINZ_PARAM_TITLE = "tag-lookup.title";
    public static final String MUSICBRAINZ_PARAM_ARTIST = "tag-lookup.artist";
    public static final String MUSICBRAINZ_PARAM_RELEASE = "tag-lookup.release";
    public static final String MUSICBRAINZ_PARAM_FILENAME = "tag-lookup.filename";

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private final AtomicInteger mProgressTaskCount = new AtomicInteger(0);

    private UntaggedListFragment mUntaggedListFragment;
    private TaggerFragment mTaggerFragment;
    private int mPort = 8000;
    private int mPrimaryColor;

    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mCustomTabsClient;
    private CustomTabsServiceConnection mConnection;
    private String mPackageNameToBind;

    private WebServer mWebServer = null;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.progress_bar)
    ProgressBar mProgressBar;
    @Bind(R.id.view_pager)
    ViewPager mViewPager;
    @Bind(R.id.fab)
    FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        mPrimaryColor = ContextCompat.getColor(this, R.color.colorPrimary);

        GenericFragmentPagerAdapter pagerAdapter = new GenericFragmentPagerAdapter(getSupportFragmentManager());

        mUntaggedListFragment = new UntaggedListFragment();
        pagerAdapter.addFragment(mUntaggedListFragment);

        mTaggerFragment = new TaggerFragment();
        pagerAdapter.addFragment(mTaggerFragment);

        mViewPager.addOnPageChangeListener(this);
        mViewPager.setAdapter(pagerAdapter);

        onPageSelected(0);

        executeProgressTask(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        final ServerSocket serverSocket = new ServerSocket(0);
                        final int localPort = serverSocket.getLocalPort();
                        Log.d(TAG, "Trying port " + localPort + " for web server");
                        serverSocket.close();
                        mWebServer = new WebServer(localPort);
                        Log.d(TAG, "Starting web server");
                        mWebServer.start();
                        Log.d(TAG, "Started on port " + localPort);
                        mPort = localPort;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } while (mWebServer == null);
            }
        });

        mPackageNameToBind = CustomTabsHelper.getPackageNameToUse(this);
        bindCustomTabsService();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        mUntaggedListFragment.setOnSectionLongClickListener(new UntaggedListAdapter.OnSectionLongClickListener() {
            @Override
            public void onSectionLongClick(final UntaggedListAdapter adapter, final UntaggedListAdapter.UntaggedRelease untaggedRelease) {
                CharSequence[] items = new CharSequence[]{"Search in browser", "Tag"};
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(untaggedRelease.getName())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        Uri.Builder uriBuilder = new Uri.Builder()
                                                .scheme(MUSICBRAINZ_QUERY_SCHEME)
                                                .authority(MUSICBRAINZ_DOMAIN)
                                                .appendPath(MUSICBRAINZ_TAG_LOOKUP)
                                                .appendQueryParameter(MUSICBRAINZ_PARAM_PORT, Integer.toString(mPort));

                                        if (untaggedRelease.album != null) {
                                            uriBuilder.appendQueryParameter(MUSICBRAINZ_PARAM_RELEASE, untaggedRelease.album);
                                        }

                                        if (untaggedRelease.artist != null) {
                                            uriBuilder.appendQueryParameter(MUSICBRAINZ_PARAM_ARTIST, untaggedRelease.artist);
                                        }

                                        launchPage(uriBuilder.build());
                                        break;

                                    case 1:
                                        adapter.setMarked(untaggedRelease, true);
                                        mTaggerFragment.setOnReleaseClickListener(new TaggerListAdapter.OnReleaseTagClickListener() {
                                            @Override
                                            public void onReleaseTagClick(final TaggerListAdapter taggerListAdapter, final TaggerListAdapter.ReleaseTag release) {
                                                adapter.setMarked(untaggedRelease, false);
                                                new AlertDialog.Builder(MainActivity.this)
                                                        .setTitle(release.release.getTitle())
                                                        .setMessage("Tag?")
                                                        .setNegativeButton(android.R.string.cancel, null)
                                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                executeProgressTask(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        taggerListAdapter.setUntaggedRelease(release, untaggedRelease);
                                                                        adapter.removeUntaggedRelease(untaggedRelease);
                                                                    }
                                                                });
                                                            }
                                                        }).show();
                                            }
                                        });
                                        mViewPager.setCurrentItem(1, true);
                                }
                            }
                        }).show();
            }
        });
        mUntaggedListFragment.setOnItemLongClickListener(new UntaggedListAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(final UntaggedListAdapter adapter, final UntaggedListAdapter.UntaggedTrack untaggedTrack) {
                CharSequence[] items = new CharSequence[]{"Search in browser", "Tag"};
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(untaggedTrack.title)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        Uri.Builder uriBuilder = new Uri.Builder()
                                                .scheme(MUSICBRAINZ_QUERY_SCHEME)
                                                .authority(MUSICBRAINZ_DOMAIN)
                                                .appendPath(MUSICBRAINZ_TAG_LOOKUP)
                                                .appendQueryParameter(MUSICBRAINZ_PARAM_PORT, Integer.toString(mPort))
                                                .appendQueryParameter(MUSICBRAINZ_PARAM_FILENAME, untaggedTrack.file.getName());

                                        if (untaggedTrack.title != null) {
                                            uriBuilder.appendQueryParameter(MUSICBRAINZ_PARAM_TITLE, untaggedTrack.title);
                                        }

                                        if (untaggedTrack.album != null) {
                                            uriBuilder.appendQueryParameter(MUSICBRAINZ_PARAM_RELEASE, untaggedTrack.album);
                                        }

                                        if (untaggedTrack.artist != null) {
                                            uriBuilder.appendQueryParameter(MUSICBRAINZ_PARAM_ARTIST, untaggedTrack.artist);
                                        }

                                        launchPage(uriBuilder.build());
                                        break;

                                    case 1:
                                        adapter.setMarked(untaggedTrack, true);
                                        mTaggerFragment.setOnTrackClickListener(new TaggerListAdapter.OnTrackTagClickListener() {
                                            @Override
                                            public void onTrackTagClick(final TaggerListAdapter taggerListAdapter, final TaggerListAdapter.ReleaseTag releaseTag, final TaggerListAdapter.TrackTag track) {
                                                adapter.setMarked(untaggedTrack, false);
                                                new AlertDialog.Builder(MainActivity.this)
                                                        .setMessage("Tag?")
                                                        .setNegativeButton(android.R.string.cancel, null)
                                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                executeProgressTask(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        taggerListAdapter.setUntaggedTrack(releaseTag, track, untaggedTrack);
                                                                        adapter.removeUntaggedTrack(untaggedTrack);
                                                                    }
                                                                });
                                                            }
                                                        }).show();
                                            }
                                        });

                                        mViewPager.setCurrentItem(1, true);
                                }
                            }
                        }).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mWebServer.stop();
        unbindCustomTabsService();
        super.onDestroy();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        Log.d(TAG, "Page selected: " + position);
        switch (position) {
            case 0:
                mFab.setImageResource(R.drawable.ic_add_24dp);
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddFilesScreen();
                    }
                });
                break;

            case 1:
                mFab.setImageResource(R.drawable.ic_save_24dp);
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Save tags")
                                .setMessage("Do you want to save the tags? This will overwrite current tag data.")
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mTaggerFragment.saveTags();
                                    }
                                }).show();
                    }
                });
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void showAddFilesScreen() {
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.EXTRA_DIR, Environment.getExternalStorageDirectory());
        startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE);
    }

    private void onProgressTaskStarted() {
        final int tasks = mProgressTaskCount.getAndIncrement();
        Log.d(TAG, "Progress task started: " + (tasks + 1) + " active");
        if (tasks == 0) {
            mProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    if (mProgressTaskCount.get() == 1) {
                        Log.d(TAG, "Showing progressbar");
                        mProgressBar.animate().alpha(1);
                    } else {
                        Log.d(TAG, "Progressbar already visible");
                    }
                }
            });
        }
    }

    private void onProgressTaskEnded() {
        final int tasks = mProgressTaskCount.getAndDecrement();
        Log.d(TAG, "Progress task ended: " + (tasks - 1) + " left");
        if (tasks == 0) {
            mProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    if (mProgressTaskCount.get() == 0) {
                        Log.d(TAG, "Hiding progressbar");
                        mProgressBar.animate().alpha(0);
                    } else {
                        Log.d(TAG, "Not hiding progrssbar yet");
                    }
                }
            });
        }
    }

    private void executeProgressTask(final Runnable runnable) {
        onProgressTaskStarted();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                onProgressTaskEnded();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            executeProgressTask(new Runnable() {
                @Override
                public void run() {
                    Set<File> chosenFiles = (Set<File>) data.getSerializableExtra(FilePickerActivity.EXTRA_FILES);
                    mUntaggedListFragment.addFiles(chosenFiles);
                }
            });

            if (mCustomTabsClient != null) {
                mCustomTabsClient.warmup(0);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private CustomTabsSession getSession() {
        if (mCustomTabsClient == null) {
            mCustomTabsSession = null;
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mCustomTabsClient.newSession(new NavigationCallback());
        }
        return mCustomTabsSession;
    }

    private void bindCustomTabsService() {
        if (mCustomTabsClient != null) return;
        if (TextUtils.isEmpty(mPackageNameToBind)) {
            mPackageNameToBind = CustomTabsHelper.getPackageNameToUse(this);
            if (mPackageNameToBind == null) return;
        }
        mConnection = new ServiceConnection(this);
        boolean ok = CustomTabsClient.bindCustomTabsService(this, mPackageNameToBind, mConnection);
        if (!ok) {
            mConnection = null;
        }
    }

    private void unbindCustomTabsService() {
        if (mConnection == null) return;
        unbindService(mConnection);
        mCustomTabsClient = null;
        mCustomTabsSession = null;
    }

    private void launchPage(String url) {
        launchPage(Uri.parse(url));
    }

    private void launchPage(Uri uri) {
        new CustomTabsIntent.Builder()
                .setToolbarColor(mPrimaryColor)
                .build()
                .launchUrl(this, uri);
    }

    @Override
    public void onServiceConnected(CustomTabsClient client) {
        mCustomTabsClient = client;
    }

    @Override
    public void onServiceDisconnected() {
        mCustomTabsClient = null;
    }

    private class WebServer extends NanoHTTPD {
        public WebServer(int port) {
            super(port);
        }

        public WebServer(String hostname, int port) {
            super(hostname, port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> params = session.getParms();
            Log.d(TAG, "Serve session; params: " + params);

            mTaggerFragment.onBrowserLookupResult(params.get("id"));
            return super.serve(session);
        }
    }

    private static class NavigationCallback extends CustomTabsCallback {
        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) {
            Log.w(TAG, "onNavigationEvent: Code = " + navigationEvent);
        }
    }
}
