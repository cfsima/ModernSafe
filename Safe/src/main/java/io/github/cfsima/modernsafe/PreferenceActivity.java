package io.github.cfsima.modernsafe;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import io.github.cfsima.modernsafe.intents.CryptoIntents;

import java.io.File;
import java.util.List;

public class PreferenceActivity extends AppCompatActivity {

    public static final String IO_METHOD_DOCUMENT_PROVIDER = "document_provider";
    public static final String IO_METHOD_FILE = "file";
    public static final String PREFERENCE_ALLOW_EXTERNAL_ACCESS = "external_access";
    public static final String PREFERENCE_LOCK_TIMEOUT = "lock_timeout";
    public static final String PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE = "5";
    public static final String PREFERENCE_LOCK_ON_SCREEN_LOCK = "lock_on_screen_lock";
    public static final String PREFERENCE_FIRST_TIME_WARNING = "first_time_warning";
    public static final String PREFERENCE_KEYPAD = "keypad";
    public static final String PREFERENCE_KEYPAD_MUTE = "keypad_mute";
    public static final String PREFERENCE_LAST_BACKUP_JULIAN = "last_backup_julian";
    public static final String PREFERENCE_LAST_AUTOBACKUP_CHECK = "last_autobackup_check";
    public static final String PREFERENCE_AUTOBACKUP = "autobackup";
    public static final String PREFERENCE_AUTOBACKUP_DAYS = "autobackup_days";
    public static final String PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE = "7";
    public static final String PREFERENCE_BACKUP_PATH = "backup_path";
    public static final String PREFERENCE_BACKUP_DOCUMENT = "backup_document";
    public static final String PREFERENCE_BACKUP_METHOD = "backup_method";
    public static final String OISAFE_XML = "oisafe.xml";
    public static final String PREFERENCE_EXPORT_PATH = "export_path";
    public static final String PREFERENCE_EXPORT_DOCUMENT = "export_document";
    public static final String PREFERENCE_EXPORT_METHOD = "export_method";
    public static final String OISAFE_CSV = "oisafe.csv";
    public static final int DIALOG_DOWNLOAD_OI_FILEMANAGER = 0;
    private static final int REQUEST_BACKUP_FILENAME = 0;
    private static final int REQUEST_BACKUP_DOCUMENT = 1;
    private static final int REQUEST_EXPORT_FILENAME = 2;
    private static final int REQUEST_EXPORT_DOCUMENT = 3;
    private static String TAG = "PreferenceActivity";
    Intent frontdoor;
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT");
                }
                startActivity(frontdoor);
            }
        }
    };
    private Intent restartTimerIntent = null;

    static String getBackupPath(Context context) {
        String path = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_BACKUP_PATH, null);
        if (path == null) {
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                return new File(dir, OISAFE_XML).getAbsolutePath();
            } else {
                return new File(context.getFilesDir(), OISAFE_XML).getAbsolutePath();
            }
        }
        return path;
    }

    static void setBackupPathAndMethod(Context context, String path) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_BACKUP_PATH, path);
        editor.putString(PREFERENCE_BACKUP_METHOD, IO_METHOD_FILE);
        editor.apply();
    }

    public static String getBackupDocument(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_BACKUP_DOCUMENT, null);
    }

    static void setBackupDocumentAndMethod(Context context, String uriString) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_BACKUP_DOCUMENT, uriString);
        editor.putString(PREFERENCE_BACKUP_METHOD, IO_METHOD_DOCUMENT_PROVIDER);
        editor.apply();
    }

    static String getExportPath(Context context) {
        String path = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_EXPORT_PATH, null);
        if (path == null) {
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                return new File(dir, OISAFE_CSV).getAbsolutePath();
            } else {
                return new File(context.getFilesDir(), OISAFE_CSV).getAbsolutePath();
            }
        }
        return path;
    }

    static void setExportPathAndMethod(Context context, String path) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_EXPORT_PATH, path);
        editor.putString(PREFERENCE_EXPORT_METHOD, IO_METHOD_FILE);
        editor.apply();
    }

    static String getExportDocument(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_EXPORT_DOCUMENT, null);
    }

    static void setExportDocumentAndMethod(Context context, String uriString) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_EXPORT_DOCUMENT, uriString);
        editor.putString(PREFERENCE_EXPORT_METHOD, IO_METHOD_DOCUMENT_PROVIDER);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences);
        frontdoor = new Intent(this, FrontDoor.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!AuthManager.isSignedIn()) {
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        ContextCompat.registerReceiver(this, mIntentReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(mIntentReceiver);
        } catch (IllegalArgumentException e) {
            //if (debug) Log.d(TAG,"IllegalArgumentException");
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onUserInteraction()");
        }

        if (!AuthManager.isSignedIn()) {
//			startActivity(frontdoor);
        } else {
            if (restartTimerIntent != null) {
                sendBroadcast(restartTimerIntent);
            }
        }
    }

    /**
     * Handler for when a MenuItem is selected from the Activity.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
        switch (requestCode) {
            case REQUEST_BACKUP_FILENAME:
                if (resultCode == RESULT_OK) {
                    setBackupPathAndMethod(this, i.getData().getPath());
                }
                break;
            case REQUEST_BACKUP_DOCUMENT:
                if (resultCode == RESULT_OK) {
                    setBackupDocumentAndMethod(this, i.getData().getPath());
                }
                break;

            case REQUEST_EXPORT_FILENAME:
                if (resultCode == RESULT_OK) {
                    setExportPathAndMethod(this, i.getData().getPath());
                }
                break;
            case REQUEST_EXPORT_DOCUMENT:
                if (resultCode == RESULT_OK) {
                    setExportDocumentAndMethod(this, i.getData().getPath());
                }
                break;
        }
    }



    public static class PreferenceFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);

            Preference backupPathPref = findPreference(PREFERENCE_BACKUP_PATH);
            backupPathPref.setOnPreferenceClickListener(
                    pref -> {
                        Intent intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_BACKUP, OISAFE_XML);
                        try {
                            startActivityForResult(intent, REQUEST_BACKUP_FILENAME);
                        } catch (android.content.ActivityNotFoundException e) {
                            Toast.makeText(getActivity(), R.string.restore_error, Toast.LENGTH_LONG).show();
                        }
                        return false;
                    }
            );

            Preference exportPathPref = findPreference(PREFERENCE_EXPORT_PATH);
            exportPathPref.setOnPreferenceClickListener(
                    pref -> {
                        Intent intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_EXPORT, OISAFE_CSV);
                        try {
                            startActivityForResult(intent, REQUEST_EXPORT_FILENAME);
                        } catch (android.content.ActivityNotFoundException e) {
                            Toast.makeText(getActivity(), R.string.restore_error, Toast.LENGTH_LONG).show();
                        }
                        return false;
                    }
            );

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            changePreferenceSummaryToCurrentValue(backupPathPref, getBackupPath(getActivity()));
            changePreferenceSummaryToCurrentValue(exportPathPref, getExportPath(getActivity()));
        }





        private void changePreferenceSummaryToCurrentValue(Preference pref, String value) {
            pref.setSummary(value);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            if (key.equals(PREFERENCE_BACKUP_PATH)) {
                changePreferenceSummaryToCurrentValue(
                        findPreference(PREFERENCE_BACKUP_PATH),
                        getBackupPath(getActivity())
                );
            } else if (key.equals(PREFERENCE_EXPORT_PATH)) {
                changePreferenceSummaryToCurrentValue(
                        findPreference(PREFERENCE_EXPORT_PATH),
                        getExportPath(getActivity())
                );
            }
        }

    }
}
