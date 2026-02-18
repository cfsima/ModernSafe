package io.github.cfsima.modernsafe

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme

class PassView : AppCompatActivity() {

    companion object {
        @JvmField var entryEdited = false
        private const val TAG = "PassView"
    }

    private val viewModel: PassViewViewModel by viewModels()

    private val editPassLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PassEdit.RESULT_DELETED) {
            entryEdited = true
            finish()
        } else if (result.resultCode == RESULT_OK || PassEditFragment.entryEdited) {
            entryEdited = true
            viewModel.reloadCurrent()
        }
    }

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                startFrontDoor()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Secure flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        entryEdited = false
        supportActionBar?.hide()

        val extras = intent.extras
        val savedId = savedInstanceState?.getLong(PassList.KEY_ID) ?: extras?.getLong(PassList.KEY_ID)
        val savedCatId = savedInstanceState?.getLong(PassList.KEY_CATEGORY_ID) ?: extras?.getLong(PassList.KEY_CATEGORY_ID)
        val savedRowIds = savedInstanceState?.getLongArray(PassList.KEY_ROWIDS) ?: extras?.getLongArray(PassList.KEY_ROWIDS)
        val savedPosition = savedInstanceState?.getInt(PassList.KEY_LIST_POSITION, -1) ?: extras?.getInt(PassList.KEY_LIST_POSITION, -1) ?: -1

        if ((savedId == null && savedPosition == -1) || savedCatId == null) {
            finish()
            return
        }

        viewModel.initialize(savedId, savedCatId, savedRowIds, savedPosition)

        setContent {
            OISafeTheme {
                val passEntry by viewModel.currentPassEntry.collectAsState()

                val packageAccess by viewModel.packageAccess.collectAsState()

                PassViewScreen(
                    passEntry = passEntry,
                    packageAccess = packageAccess,
                    onBack = { finish() },
                    onEdit = { entry ->
                        entry?.let {
                           val intent = Intent(this, PassEdit::class.java)
                           intent.putExtra(PassList.KEY_ID, it.id)
                           intent.putExtra(PassList.KEY_CATEGORY_ID, it.category)
                           editPassLauncher.launch(intent)
                        }
                    },
                    onDelete = { entry ->
                        entry?.let {
                             confirmDelete()
                        }
                    },
                    onCopy = { label, text ->
                        copyToClipboard(label, text)
                    },
                    onLaunchUrl = { url ->
                        launchUrl(url)
                    },
                    onNext = { viewModel.loadNext() },
                    onPrev = { viewModel.loadPrevious() },
                    hasNext = viewModel.hasNext(),
                    hasPrev = viewModel.hasPrevious()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!checkSignedIn()) {
            return
        }
        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Restart timer
        sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver might not be registered if onResume returned early
             Log.w(TAG, "Failed to unregister logoutReceiver", e)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (CategoryList.isSignedIn()) {
             sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
    }

    private fun checkSignedIn(): Boolean {
        if (!CategoryList.isSignedIn()) {
            startFrontDoor()
            return false
        }
        return true
    }

    private fun startFrontDoor() {
        val intent = Intent(this, Safe::class.java)
        intent.action = CryptoIntents.ACTION_AUTOLOCK
        startActivity(intent)
        finish()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        viewModel.updateLastUsedPassword(text)
        Toast.makeText(this, "$label ${getString(R.string.copied_to_clipboard)}", Toast.LENGTH_SHORT).show()
    }

    private fun launchUrl(url: String) {
        var finalUrl = url
        if (finalUrl.isEmpty() || finalUrl == "http://") {
             Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
             return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                finalUrl = "http://$finalUrl"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                startActivity(intent)
            } catch (e2: ActivityNotFoundException) {
                 Toast.makeText(this, getString(R.string.invalid_website), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete() {
         AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_password_title)
            .setMessage(R.string.dialog_delete_password_msg)
            .setPositiveButton(R.string.yes) { _, _ ->
                 viewModel.deleteCurrentEntry()
                 setResult(RESULT_OK)
                 finish()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}
