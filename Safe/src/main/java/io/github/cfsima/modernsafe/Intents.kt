package io.github.cfsima.modernsafe

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract

object Intents {
    @JvmStatic
    fun createPickFileIntent(filename: String, titleResource: Int): Intent {
        val intent = Intent("org.openintents.action.PICK_FILE")
        intent.data = Uri.parse("file://$filename")
        intent.putExtra("org.openintents.extra.TITLE", titleResource)
        return intent
    }

    @JvmStatic
    fun createCreateDocumentIntent(mimeType: String, filename: String): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, filename)
        return intent
    }

    @JvmStatic
    fun createOpenDocumentIntents(mimeType: String, backupDocument: String?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        if (!backupDocument.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(backupDocument))
        }
        return intent
    }
}
