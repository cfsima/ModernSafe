package io.github.cfsima.modernsafe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme
import java.io.IOException

class Help : AppCompatActivity() {

    private val debug = false
    private val TAG = "Help"
    private val CLOSE_HELP_INDEX = Menu.FIRST

    private lateinit var frontdoor: Intent
    private var restartTimerIntent: Intent? = null

    private val mIntentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                if (debug) {
                    Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT")
                }
                startActivity(frontdoor)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        frontdoor = Intent(this, FrontDoor::class.java)
        frontdoor.action = CryptoIntents.ACTION_AUTOLOCK
        restartTimerIntent = Intent(CryptoIntents.ACTION_RESTART_TIMER)

        val title = "${getString(R.string.app_name)} - ${getString(R.string.help)}"
        setTitle(title)

        // Read help.html
        var helpText = ""
        try {
            assets.open("help.html").use { stream ->
                helpText = stream.bufferedReader().use { it.readText() }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        setContent {
            OISafeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HelpScreen(helpText)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (debug) Log.d(TAG, "onResume()")

        if (!AuthManager.isSignedIn()) {
            startActivity(frontdoor)
            return
        }

        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, mIntentReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(mIntentReceiver)
        } catch (e: IllegalArgumentException) {
            // ignore
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, CLOSE_HELP_INDEX, 0, R.string.close)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setShortcut('0', 'w')
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == CLOSE_HELP_INDEX) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (debug) Log.d(TAG, "onUserInteraction()")

        if (AuthManager.isSignedIn()) {
            restartTimerIntent?.let { sendBroadcast(it) }
        }
    }
}

@Composable
fun HelpScreen(htmlContent: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                loadData(htmlContent, "text/html", "utf-8")
            }
        },
        update = { webView ->
            webView.loadData(htmlContent, "text/html", "utf-8")
        }
    )
}
