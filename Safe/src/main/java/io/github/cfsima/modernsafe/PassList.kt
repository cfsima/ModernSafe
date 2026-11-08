package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity

/**
 * Legacy activity replaced by PassListRoute in ModernSafeNavHost.
 */
@Deprecated("Migrated to Compose Navigation within MainActivity. ModernSafeNavHost now manages this destination.")
class PassList : AppCompatActivity() {

    companion object {
        const val VIEW_PASSWORD_INDEX = Menu.FIRST
        const val EDIT_PASSWORD_INDEX = Menu.FIRST + 1
        const val ADD_PASSWORD_INDEX = Menu.FIRST + 2
        const val DEL_PASSWORD_INDEX = Menu.FIRST + 3
        const val MOVE_PASSWORD_INDEX = Menu.FIRST + 4

        const val REQUEST_VIEW_PASSWORD = 1
        const val REQUEST_EDIT_PASSWORD = 2
        const val REQUEST_ADD_PASSWORD = 3
        const val REQUEST_MOVE_PASSWORD = 4

        const val KEY_ID = "id"
        const val KEY_CATEGORY_ID = "categoryId"
        const val KEY_ROWIDS = "rowids"
        const val KEY_LIST_POSITION = "position"

        private const val TAG = "PassList"
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
