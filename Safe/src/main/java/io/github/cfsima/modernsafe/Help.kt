@file:Suppress("MoveLambdaOutsideParentheses")

package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Legacy activity replaced by HelpRoute in ModernSafeNavHost.
 */
@Deprecated("Migrated to Compose Navigation within MainActivity. ModernSafeNavHost now manages this destination.")
class Help : AppCompatActivity() {

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

@Composable
fun HelpScreen(
    htmlContent: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                loadData(htmlContent, "text/html", "utf-8")
            }
        },
        update = { webView ->
            webView.loadData(htmlContent, "text/html", "utf-8")
        },
    )
}
