package io.github.cfsima.modernsafe

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme

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

    private val viewModel: PassListViewModel by viewModels()
    private var categoryId: Long = -1L

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                startFrontDoor()
            }
        }
    }

    private val addPasswordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
             viewModel.reload()
        }
    }

    private val editPasswordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
         if (result.resultCode == Activity.RESULT_OK) {
             viewModel.reload()
         }
    }

    private val viewPasswordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
         if (result.resultCode == Activity.RESULT_OK || PassView.entryEdited) {
             viewModel.reload()
         }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 11) { // HONEYCOMB
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        categoryId = savedInstanceState?.getLong(CategoryList.KEY_ID) ?: intent.getLongExtra(CategoryList.KEY_ID, -1L)

        if (categoryId == -1L) {
             finish()
             return
        }

        viewModel.load(categoryId)

        setContent {
            OISafeTheme {
                val uiState by viewModel.uiState.collectAsState()

                var showDeleteDialog by remember { mutableStateOf<PassEntry?>(null) }
                var showMoveDialog by remember { mutableStateOf<PassEntry?>(null) }

                PassListScreen(
                    uiState = uiState,
                    onAddPassword = {
                        val intent = Intent(this, PassEdit::class.java)
                        intent.putExtra(KEY_ID, -1L)
                        intent.putExtra(KEY_CATEGORY_ID, categoryId)
                        addPasswordLauncher.launch(intent)
                    },
                    onViewPassword = { entry ->
                        val intent = Intent(this, PassView::class.java)
                        intent.putExtra(KEY_ID, entry.id)
                        intent.putExtra(KEY_CATEGORY_ID, categoryId)

                        val rowIds = uiState.entries.map { it.id }.toLongArray()
                        intent.putExtra(KEY_ROWIDS, rowIds)

                        val position = uiState.entries.indexOfFirst { it.id == entry.id }
                        intent.putExtra(KEY_LIST_POSITION, position)

                        viewPasswordLauncher.launch(intent)
                    },
                    onEditPassword = { entry ->
                        val intent = Intent(this, PassEdit::class.java)
                        intent.putExtra(KEY_ID, entry.id)
                        intent.putExtra(KEY_CATEGORY_ID, categoryId)
                        editPasswordLauncher.launch(intent)
                    },
                    onDeletePassword = { entry ->
                        showDeleteDialog = entry
                    },
                    onMovePassword = { entry ->
                        showMoveDialog = entry
                    },
                    onBack = {
                        val intent = Intent(this, CategoryList::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finish()
                    }
                )

                if (showDeleteDialog != null) {
                    DeletePasswordDialog(
                        onConfirm = {
                            viewModel.deleteEntry(showDeleteDialog!!)
                            showDeleteDialog = null
                        },
                        onDismiss = { showDeleteDialog = null }
                    )
                }

                if (showMoveDialog != null) {
                    val categories = remember(uiState.categoryName) { viewModel.getAvailableCategories() }
                    MovePasswordDialog(
                        categories = categories,
                        onCategorySelected = { newCatId ->
                            val entry = showMoveDialog!!
                            val categoryName = categories.entries.find { it.value == newCatId }?.key
                            viewModel.moveEntry(entry, newCatId)
                            if (categoryName != null) {
                                 Toast.makeText(this@PassList, getString(R.string.moved_to, categoryName), Toast.LENGTH_LONG).show()
                            }
                            showMoveDialog = null
                        },
                        onDismiss = { showMoveDialog = null }
                    )
                }
            }
        }

        sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
        if (!checkSignedIn()) {
            return
        }

        Passwords.Initialize(this)

        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        viewModel.reload()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            // ignore
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(CategoryList.KEY_ID, categoryId)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (AuthManager.isSignedIn()) {
            sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, Search::class.java))
        return true
    }

    private fun checkSignedIn(): Boolean {
        if (!AuthManager.isSignedIn()) {
            startFrontDoor()
            return false
        }
        return true
    }

    private fun startFrontDoor() {
        val intent = Intent(this, FrontDoor::class.java)
        intent.action = CryptoIntents.ACTION_AUTOLOCK
        startActivity(intent)
        finish()
    }
}
