package io.github.cfsima.modernsafe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cfsima.modernsafe.service.AutoLockService
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme
import io.github.cfsima.modernsafe.util.VersionUtils
import kotlinx.coroutines.delay

class LogOffActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = VersionUtils.getApplicationName(this)
        val version = VersionUtils.getVersionNumber(this)
        val headerText = "$appName $version"

        setContent {
            OISafeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LogOffScreen(
                        headerText = headerText,
                        onLogOff = { logOffAndFinish() },
                        onGoToPWS = { goToPWS() }
                    )
                }
            }
        }
    }

    private fun logOffAndFinish() {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (cb.hasPrimaryClip()) {
            val clip = cb.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text.toString()
                if (text == AuthManager.lastUsedPassword) {
                     cb.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }

        val autoLockIntent = Intent(applicationContext, AutoLockService::class.java)
        stopService(autoLockIntent)

        AuthManager.setSignedOut()
        finish()
    }

    private fun goToPWS() {
        val intent = Intent(this, FrontDoor::class.java)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.action = Intent.ACTION_MAIN
        startActivity(intent)
        finish()
    }
}

@Composable
fun LogOffScreen(
    headerText: String,
    onLogOff: () -> Unit,
    onGoToPWS: () -> Unit
) {
    var timeRemaining by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            timeRemaining = AutoLockService.getTimeRemaining()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
