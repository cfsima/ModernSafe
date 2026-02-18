package io.github.cfsima.modernsafe;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import static android.content.Intent.*;

public class Intents {
    static Intent createPickFileIntent(String filename, int titleResource) {
        Intent intent;
        intent = new Intent("org.openintents.action.PICK_FILE");
        intent.setData(Uri.parse("file://" + filename));
        intent.putExtra("org.openintents.extra.TITLE", titleResource);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    static Intent createCreateDocumentIntent(String mimeType, String filename) {
        Intent intent = new Intent(ACTION_CREATE_DOCUMENT);
        intent.addCategory(CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(EXTRA_TITLE, filename);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Intent createOpenDocumentIntents(String mimeType, String backupDocument) {
        Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
        intent.addCategory(CATEGORY_OPENABLE);
        intent.setType(mimeType);
        if (backupDocument != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(backupDocument));
        }
        return intent;
    }
}
