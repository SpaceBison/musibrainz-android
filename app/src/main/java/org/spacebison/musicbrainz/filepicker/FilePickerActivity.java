package org.spacebison.musicbrainz.filepicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.spacebison.musicbrainz.R;
import org.spacebison.musicbrainz.adapter.FilePickerAdapter;

import java.io.File;
import java.io.Serializable;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by cmb on 09.03.16.
 */
public class FilePickerActivity extends AppCompatActivity implements FilePickerAdapter.Listener, FilePickerAdapter.OnItemCheckedListener {
    public static final String EXTRA_DIR = "org.spacebison.musicbrainz.EXTRA_DIR";
    public static final String EXTRA_FILES = "org.spacebison.musicbrainz.EXTRA_FILES";

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.app_bar)
    AppBarLayout mAppBar;

    private FilePickerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        final File dir = (File) getIntent().getSerializableExtra(EXTRA_DIR);
        mAdapter = new FilePickerAdapter(dir);
        mAdapter.setListener(this);
        mAdapter.setOnItemCheckedListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        onDirChanged(dir);
    }

    @Override
    public void onDirChanged(File dir) {
        if (dir != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(dir.getName());
            }
        }

        mAppBar.setExpanded(true);
        mAppBar.postInvalidate();
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.getDir().getParent() == null) {
            super.onBackPressed();
        } else {
            mAdapter.goUp();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_file_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_go_up:
                mAdapter.goUp();
                return true;

            case R.id.action_done:
                Intent intent = new Intent();
                intent.putExtra(EXTRA_FILES, (Serializable) mAdapter.getCheckedFiles());
                setResult(RESULT_OK, intent);
                finish();
                return true;

            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemChecked(File file, boolean isChecked) {
        mAppBar.setExpanded(true);
    }
}
