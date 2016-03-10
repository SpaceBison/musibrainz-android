package org.spacebison.musicbrainz;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FileChooserActivity extends AppCompatActivity {
    private static final int CHOOSE_FILE_REQUEST_CODE = 1;
    private static final String TAG = "FileChooserActivity";
    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;

    FileAdapter mAdapter = new FileAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.fromFile(Environment.getExternalStorageDirectory());
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setDataAndType(uri, "*/*");
                startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE);
            }
        });

        0x7f
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.d(TAG, "Chosen uri: " + uri);
            try {
                AssetFileDescriptor assetFileDescriptor = getContentResolver().openAssetFileDescriptor(uri, "rw");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            mAdapter.getFiles().add(new File(uri.getPath()).getAbsoluteFile());
            mAdapter.notifyDataSetChanged();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
