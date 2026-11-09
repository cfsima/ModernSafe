package io.github.cfsima.modernsafe

import android.content.Intent
import android.provider.DocumentsContract
import androidx.core.net.toUri

object Intents {

    const val MIME_TYPE_BACKUP = "text/xml"
    const val MIME_TYPE_EXPORT = "text/csv"
    const val MIME_TYPE_ANY_TEXT = "text/*"
    const val MAX_CATEGORIES = 256

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
        if (!backupDocument.isNullOrEmpty()) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, backupDocument.toUri())
        }
        return intent
    }
}
