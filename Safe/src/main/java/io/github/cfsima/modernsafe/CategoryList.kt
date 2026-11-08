package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Legacy activity replaced by CategoryListRoute in ModernSafeNavHost.
 */
@Deprecated("Migrated to Compose Navigation within MainActivity. ModernSafeNavHost now manages this destination.")
class CategoryList : AppCompatActivity() {

    companion object {
        const val TAG = "CategoryList"
        const val KEY_ID = "id"

        const val REQUEST_OPEN_CATEGORY = 1
        const val REQUEST_RESTORE = 2
        const val REQUEST_IMPORT_FILENAME = 3
        const val REQUEST_EXPORT_FILENAME = 4
        const val REQUEST_BACKUP_FILENAME = 5
        const val REQUEST_IMPORT_DOCUMENT = 6
        const val REQUEST_EXPORT_DOCUMENT = 7
        const val REQUEST_BACKUP_DOCUMENT = 8
        const val REQUEST_RESTORE_FILENAME = 9
        const val REQUEST_RESTORE_DOCUMENT = 10

        const val MIME_TYPE_BACKUP = "text/xml"
        const val MIME_TYPE_EXPORT = "text/csv"
        const val MIME_TYPE_ANY_TEXT = "text/*"

        const val MAX_CATEGORIES = 256
        const val PASSWORDSAFE_IMPORT_FILENAME = "/passwordsafe.csv"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forwardIntent = Intent(this, MainActivity::class.java).apply {
            action = intent.action
            data = intent.data
            intent.extras?.let { putExtras(it) }
            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        }
        startActivity(forwardIntent)
        finish()
    }
}
