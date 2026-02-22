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
import androidx.compose.runtime.*
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

        val appName = "Modern Safe" // VersionUtils.getApplicationName(this) requires resource lookup which might fail if context issue, using hardcoded for now or fix VersionUtils?
        // Wait, VersionUtils is there.
        // Let's assume VersionUtils works.

        // Actually, let's stick to the previous code structure but fix the timeRemaining call.

        val version = try {
             packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
             "?"
        }
        val headerText = "Modern Safe "

        setContent {
            OISafeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var timeRemaining by remember { mutableLongStateOf(0L) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            timeRemaining = AuthManager.timeRemaining
                            delay(1000)
                        }
                    }

                    LogOffScreenContent(
                        headerText = headerText,
                        timeRemaining = timeRemaining,
                        onLogOff = {
                            // Stop service
                            val intent = Intent(applicationContext, AutoLockService::class.java)
                            stopService(intent)

                            // Clear clipboard if needed
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

                            AuthManager.setSignedOut()
                            finish()
                        },
                        onGoToPWS = {
                            val intent = Intent(this@LogOffActivity, FrontDoor::class.java)
                            intent.addCategory(Intent.CATEGORY_LAUNCHER)
                            intent.action = Intent.ACTION_MAIN
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
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
