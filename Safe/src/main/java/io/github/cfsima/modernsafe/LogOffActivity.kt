package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Legacy activity replaced by LogOffRoute in ModernSafeNavHost.
 */
@Deprecated("Migrated to Compose Navigation within MainActivity. ModernSafeNavHost now manages this destination.")
class LogOffActivity : AppCompatActivity() {

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
fun LogOffScreenContent(
    headerText: String,
    timeRemaining: Long,
    onLogOff: () -> Unit,
    onGoToPWS: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.passicon),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = headerText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(R.string.logoff_explanation),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLogOff,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.lock))
            }

            Button(
                onClick = onGoToPWS,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.open_safe))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val secondsTotal = timeRemaining / 1000
        val minutes = secondsTotal / 60
        val seconds = secondsTotal % 60
        val timeString = String.format("%d:%02d", minutes, seconds)

        Text(text = stringResource(R.string.lock_timeout, timeString))
    }
}
