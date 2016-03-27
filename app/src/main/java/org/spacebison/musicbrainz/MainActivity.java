package org.spacebison.musicbrainz;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
    private static final String TAG = "MainActivity";

    private UntaggedListFragment mUntaggedListFragment;
    private TaggerFragment mTaggerFragment;

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
                        mUntaggedListFragment.setPort(localPort);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } while (mWebServer == null);
            }
        }.start();
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
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mUntaggedListFragment.showAddFilesScreen();
                    }
                });
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

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
