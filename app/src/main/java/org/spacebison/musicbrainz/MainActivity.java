package org.spacebison.musicbrainz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
    private static final String TAG = "MainActivity";
    private static final int CHOOSE_FILE_REQUEST_CODE = 1;

    private UntaggedListFragment mUntaggedListFragment;
    private TaggerFragment mTaggerFragment;
    private int mPort = 8000;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.view_pager)
    ViewPager mViewPager;
    @Bind(R.id.fab)
    FloatingActionButton mFab;

    private GenericFragmentPagerAdapter mPagerAdapter;
    private WebServer mWebServer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        mPagerAdapter = new GenericFragmentPagerAdapter(getSupportFragmentManager());

        mUntaggedListFragment = new UntaggedListFragment();
        mPagerAdapter.addFragment(mUntaggedListFragment);

        mTaggerFragment = new TaggerFragment();
        mPagerAdapter.addFragment(mTaggerFragment);

        mViewPager.addOnPageChangeListener(this);
        mViewPager.setAdapter(mPagerAdapter);

        onPageSelected(0);

        new Thread("ServerSocketThread") {
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
        }.start();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        mUntaggedListFragment.setOnSectionLongClickListener(new UntaggedListAdapter.OnSectionLongClickListener() {
            @Override
            public void onSectionLongClick(final UntaggedListAdapter adapter, final UntaggedListAdapter.UntaggedRelease untaggedRelease) {
                CharSequence[] items = new CharSequence[]{"Search in browser", "Tag"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Uri.Builder uriBuilder = new Uri.Builder()
                                        .scheme("http")
                                        .authority("musicbrainz.org")
                                        .appendPath("taglookup")
                                        .appendQueryParameter("tport", Integer.toString(mPort)) // // TODO: 27.03.16 add working port
                                        .appendQueryParameter("tag-lookup.release", untaggedRelease.album)
                                        .appendQueryParameter("tag-lookup.artist", untaggedRelease.artist);
                                int color = ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);
                                startActivity(Utils.getChromeCustomTabIntent(uriBuilder.build(), color));
                                break;

                            case 1:
                                adapter.setMarked(untaggedRelease, true);
                                mTaggerFragment.setOnReleaseClickListener(new TaggerListAdapter.OnReleaseTagClickListener() {
                                    @Override
                                    public void onReleaseTagClick(final TaggerListAdapter taggerListAdapter, final TaggerListAdapter.ReleaseTag release) {
                                        new AlertDialog.Builder(MainActivity.this)
                                                .setMessage("Tag?")
                                                .setNegativeButton(android.R.string.cancel, null)
                                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        taggerListAdapter.setUntaggedRelease(release, untaggedRelease, adapter.getUntaggedTracks(untaggedRelease));
                                                        adapter.removeUntaggedRelease(untaggedRelease);
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                }).show();
                                    }
                                });
                                mViewPager.setCurrentItem(1, true);
                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mWebServer.stop();
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

    public void showAddFilesScreen() {
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.EXTRA_DIR, Environment.getExternalStorageDirectory());
        startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Set<File> chosenFiles = (Set<File>) data.getSerializableExtra(FilePickerActivity.EXTRA_FILES);
            mUntaggedListFragment.addFiles(chosenFiles);
        }

        super.onActivityResult(requestCode, resultCode, data);
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
}
