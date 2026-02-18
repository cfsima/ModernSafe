package io.github.cfsima.modernsafe

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom

data class PassGenState(
    val length: String = "12",
    val includeUpper: Boolean = true,
    val includeLower: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val generatedPassword: String = ""
)

class PassGenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PassGenState())
    val uiState: StateFlow<PassGenState> = _uiState.asStateFlow()

    private val secureRandom = SecureRandom()

    init {
        generatePassword()
    }

    fun updateLength(length: String) {
        // Only allow numeric input
        if (length.all { it.isDigit() }) {
             _uiState.update { it.copy(length = length) }
             generatePassword()
        }
    }

    fun toggleUpper(enabled: Boolean) {
        _uiState.update { it.copy(includeUpper = enabled) }
        generatePassword()
    }

    fun toggleLower(enabled: Boolean) {
        _uiState.update { it.copy(includeLower = enabled) }
        generatePassword()
    }

    fun toggleDigits(enabled: Boolean) {
        _uiState.update { it.copy(includeDigits = enabled) }
        generatePassword()
    }

    fun toggleSymbols(enabled: Boolean) {
        _uiState.update { it.copy(includeSymbols = enabled) }
        generatePassword()
    }

    fun generatePassword() {
        val state = _uiState.value
        val length = state.length.toIntOrNull() ?: 0

        if (length <= 0) {
            _uiState.update { it.copy(generatedPassword = "") }
            return
        }

        val charset = buildString {
            if (state.includeUpper) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (state.includeLower) append("abcdefghijklmnopqrstuvwxyz")
            if (state.includeDigits) append("0123456789")
            if (state.includeSymbols) append("!@#$%^&*")
        }

        if (charset.isEmpty()) {
             _uiState.update { it.copy(generatedPassword = "") }
             return
        }

        val password = buildString {
            repeat(length) {
                append(charset[secureRandom.nextInt(charset.length)])
            }
        }

        _uiState.update { it.copy(generatedPassword = password) }
    }
}
