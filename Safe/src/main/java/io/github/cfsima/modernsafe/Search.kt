package io.github.cfsima.modernsafe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme

class Search : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()

    private val passViewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // If PassView edited an entry or deleted one, refresh the search.
        if (PassView.entryEdited || result.resultCode == RESULT_OK) {
             val currentQuery = viewModel.uiState.value.query
             if (currentQuery.isNotEmpty()) {
                 viewModel.onQueryChange(currentQuery)
             }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        supportActionBar?.hide()

        setContent {
            OISafeTheme {
                val uiState by viewModel.uiState.collectAsState()

                SearchScreen(
                    uiState = uiState,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onResultClick = { index, item ->
                        val intent = Intent(this, PassView::class.java)
                        intent.putExtra(PassList.KEY_ID, item.id)
                        intent.putExtra(PassList.KEY_CATEGORY_ID, item.categoryId)
                        // Pass global row IDs for prev/next navigation
                        intent.putExtra(PassList.KEY_ROWIDS, uiState.resultIds)
                        intent.putExtra(PassList.KEY_LIST_POSITION, index)
                        passViewLauncher.launch(intent)
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AuthManager.isSignedIn()) {
            startFrontDoor()
            return
        }

        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver might not be registered
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (AuthManager.isSignedIn()) {
            sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
    }

    private fun startFrontDoor() {
        val intent = Intent(this, FrontDoor::class.java)
        intent.action = CryptoIntents.ACTION_AUTOLOCK
        startActivity(intent)
        finish()
    }
}
