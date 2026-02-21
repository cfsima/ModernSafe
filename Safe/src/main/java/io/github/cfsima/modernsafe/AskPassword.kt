package io.github.cfsima.modernsafe

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import io.github.cfsima.modernsafe.service.AutoLockService
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AskPassword : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_LOCAL = "io.github.cfsima.modernsafe.bundle.EXTRA_IS_REMOTE"
    }

    private val viewModel: AskPasswordViewModel by viewModels()

    private var mpDigitBeep: MediaPlayer? = null
    private var mpErrorBeep: MediaPlayer? = null
    private var mpSuccessBeep: MediaPlayer? = null

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
             setResult(RESULT_OK)
             finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        try {
            mpDigitBeep = MediaPlayer.create(this, R.raw.dtmf2a)
            mpErrorBeep = MediaPlayer.create(this, R.raw.click6a)
            mpSuccessBeep = MediaPlayer.create(this, R.raw.dooropening1)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val isLocal = intent.getBooleanExtra(EXTRA_IS_LOCAL, false)
        if (!isLocal) {
            Toast.makeText(this, "External Application Requesting Password", Toast.LENGTH_LONG).show()
        }

        setContent {
            OISafeTheme {
                val uiState by viewModel.uiState.collectAsState()

                if (uiState.showDatabaseError) {
                     AlertDialog(
                         onDismissRequest = { finish() },
                         title = { Text(stringResource(R.string.database_error_title)) },
                         text = { Text(stringResource(R.string.database_error_msg)) },
                         confirmButton = {
                             TextButton(onClick = { finish() }) { Text("OK") }
                         }
                     )
                }

                if (uiState.showVersionError) {
                     AlertDialog(
                         onDismissRequest = {
                             setResult(RESULT_CANCELED)
                             finish()
                         },
                         title = { Text(stringResource(R.string.database_version_error_title)) },
                         text = { Text(stringResource(R.string.database_version_error_msg)) },
                         confirmButton = {
                             TextButton(onClick = {
                                 setResult(RESULT_CANCELED)
                                 finish()
                             }) { Text("OK") }
                         }
                     )
                }

                AskPasswordScreen(
                    uiState = uiState,
                    effects = viewModel.effects,
                    onPasswordSubmit = { viewModel.onPasswordSubmit(it) },
                    onFirstTimeSetup = { pass, confirm -> viewModel.onFirstTimeSetup(pass, confirm) },
                    onToggleKeypad = { viewModel.toggleKeypadMode() },
                    onToggleMute = { viewModel.toggleMute() },
                    onKeypadInput = { viewModel.onKeypadInput(it) },
                    onKeypadDelete = { viewModel.onKeypadDelete() },
                    onKeypadEnter = { viewModel.onKeypadEnter() },
                    onRestoreClicked = { viewModel.onRestoreClicked() }
                )
            }
        }

        lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is AskPasswordEffect.UnlockSuccess -> {
                        viewModel.onUnlockSuccess()
                        val autoLockIntent = Intent(applicationContext, AutoLockService::class.java)
                        startService(autoLockIntent)
                        setResult(RESULT_OK)
                        finish()
                    }
                    is AskPasswordEffect.PlayDigitSound -> tryPlay(mpDigitBeep)
                    is AskPasswordEffect.PlayErrorSound -> tryPlay(mpErrorBeep)
                    is AskPasswordEffect.PlaySuccessSound -> tryPlay(mpSuccessBeep)
                    is AskPasswordEffect.ShakeError -> { /* Handled in Compose */ }
                    is AskPasswordEffect.ShowToast -> {
                        Toast.makeText(this@AskPassword, effect.messageResId, Toast.LENGTH_SHORT).show()
                    }
                    is AskPasswordEffect.NavigateToRestoreFirstTime -> {
                        val intent = Intent(this@AskPassword, RestoreFirstTimeActivity::class.java)
                        restoreLauncher.launch(intent)
                    }
                    is AskPasswordEffect.CloseApp -> finish()
                }
            }
        }
    }

    private fun tryPlay(mp: MediaPlayer?) {
        try {
            if (mp?.isPlaying == true) {
                mp.seekTo(0)
            }
            mp?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mpDigitBeep?.release()
        mpErrorBeep?.release()
        mpSuccessBeep?.release()
    }
}
