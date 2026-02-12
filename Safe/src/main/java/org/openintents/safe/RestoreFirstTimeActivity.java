package org.openintents.safe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.openintents.util.VersionUtils;

public class RestoreFirstTimeActivity extends AppCompatActivity {
    private Button restore;
    private Button chooseFile;
    private Button cancel;
    private String path;

    private static final int REQUEST_RESTORE = 0;
    private static final int REQUEST_CHOOSE_FILE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restore_first_time);
        path = PreferenceActivity.getBackupPath(this);
        restore = (Button) findViewById(R.id.restore);
        chooseFile = (Button) findViewById(R.id.choose_file);
        cancel = (Button) findViewById(R.id.cancel);

        ((TextView) findViewById(R.id.filename)).setText(path);
        restore.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        Intent i = new Intent(RestoreFirstTimeActivity.this, Restore.class);
                        i.putExtra(Restore.KEY_FILE_PATH, path);
                        i.putExtra(Restore.KEY_FIRST_TIME, true);
                        startActivityForResult(i, REQUEST_RESTORE);
                    }
                }
        );
        chooseFile.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        startActivityForResult(intent, REQUEST_CHOOSE_FILE);
                    }
                }
        );
        cancel.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
        );

        /* Copied from AskPassword.java - normalInit() */
        TextView header = (TextView) findViewById(R.id.entry_header);
        String version = VersionUtils.getVersionNumber(this);
        String appName = VersionUtils.getApplicationName(this);
        String head = appName + " " + version;
        header.setText(head);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_RESTORE:
                setResult(resultCode);
                finish();
                break;
            case REQUEST_CHOOSE_FILE:
                if (resultCode == RESULT_OK && data != null) {
                    path = data.getData().toString();
                    ((TextView) findViewById(R.id.filename)).setText(path);
                }
                break;
        }
    }
}
