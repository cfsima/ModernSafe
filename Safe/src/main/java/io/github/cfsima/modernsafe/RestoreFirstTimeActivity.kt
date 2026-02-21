package io.github.cfsima.modernsafe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme
import java.io.File
import io.github.cfsima.modernsafe.util.VersionUtils

class RestoreFirstTimeActivity : AppCompatActivity() {

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        setResult(result.resultCode)
        finish()
    }

    private val chooseFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, Restore::class.java)
            intent.data = it
            intent.putExtra(Restore.KEY_FIRST_TIME, true)
            restoreLauncher.launch(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultPath = Settings.getBackupPath(this)
        val fileExists = File(defaultPath).exists()

        val appName = VersionUtils.getApplicationName(this)
        val version = VersionUtils.getVersionNumber(this)
        val headerText = "$appName $version"

        setContent {
            OISafeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RestoreFirstTimeScreen(
                        headerText = headerText,
                        filePath = defaultPath,
                        fileExists = fileExists,
                        onRestore = {
                            val intent = Intent(this, Restore::class.java)
                            intent.putExtra(Restore.KEY_FILE_PATH, defaultPath)
                            intent.putExtra(Restore.KEY_FIRST_TIME, true)
                            restoreLauncher.launch(intent)
                        },
                        onChooseFile = {
                             chooseFileLauncher.launch(arrayOf("*/*"))
                        },
                        onCancel = {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RestoreFirstTimeScreen(
    headerText: String,
    filePath: String,
    fileExists: Boolean,
    onRestore: () -> Unit,
    onChooseFile: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = headerText, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = stringResource(R.string.filename))
        Text(text = filePath)

        if (!fileExists) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.restore_no_file), color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRestore, enabled = fileExists) {
            Text(stringResource(R.string.restore))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onChooseFile) {
            Text(stringResource(R.string.restore_select_file))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onCancel) {
            Text(stringResource(android.R.string.cancel))
        }
    }
}
