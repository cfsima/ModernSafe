package io.github.cfsima.modernsafe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.ui.theme.ModernSafeTheme
import java.io.IOException

class Help : AppCompatActivity() {

    private val debug = false
    private val TAG = "Help"

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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        frontdoor = Intent(this, FrontDoor::class.java)
        frontdoor.action = CryptoIntents.ACTION_AUTOLOCK
        restartTimerIntent = Intent(CryptoIntents.ACTION_RESTART_TIMER)

        val title = "${getString(R.string.app_name)} - ${getString(R.string.help)}"

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
            ModernSafeTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(title) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.close)
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        HelpScreen(helpText)
                    }
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
        modifier = Modifier.fillMaxSize(),
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
