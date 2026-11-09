package io.github.cfsima.modernsafe

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom
import kotlin.math.log2
import kotlin.random.asKotlinRandom

data class PassGenState(
    val length: Int = 16,
    val includeUpper: Boolean = true,
    val includeLower: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val generatedPassword: String = "",
    val strengthScore: Float = 0f,
    val strengthLabel: String = "",
)

class PassGenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PassGenState())
    val uiState: StateFlow<PassGenState> = _uiState.asStateFlow()

    private val secureRandom = SecureRandom()
    private val kotlinRandom = secureRandom.asKotlinRandom()

    companion object {
        const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWER = "abcdefghijklmnopqrstuvwxyz"
        const val DIGITS = "0123456789"
        const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    }

    init {
        generatePassword()
    }

    fun updateLength(length: Int) {
        val clamped = length.coerceIn(8, 64)
        _uiState.update { it.copy(length = clamped) }
        generatePassword()
    }

    fun toggleUpper(enabled: Boolean) {
        if (!enabled && (activeCategoryCount() <= 1)) return
        _uiState.update { it.copy(includeUpper = enabled) }
        generatePassword()
    }

    fun toggleLower(enabled: Boolean) {
        if (!enabled && (activeCategoryCount() <= 1)) return
        _uiState.update { it.copy(includeLower = enabled) }
        generatePassword()
    }

    fun toggleDigits(enabled: Boolean) {
        if (!enabled && (activeCategoryCount() <= 1)) return
        _uiState.update { it.copy(includeDigits = enabled) }
        generatePassword()
    }

    fun toggleSymbols(enabled: Boolean) {
        if (!enabled && (activeCategoryCount() <= 1)) return
        _uiState.update { it.copy(includeSymbols = enabled) }
        generatePassword()
    }

    fun activeCategoryCount(): Int = with(_uiState.value) {
        listOf(includeUpper, includeLower, includeDigits, includeSymbols).count { it }
    }

    fun generatePassword() {
        val state = _uiState.value
        val pool = StringBuilder()
        val guaranteed = mutableListOf<Char>()

        with(state) {
            if (includeUpper) {
                pool.append(UPPER)
                guaranteed.add(UPPER[secureRandom.nextInt(UPPER.length)])
            }
            if (includeLower) {
                pool.append(LOWER)
                guaranteed.add(LOWER[secureRandom.nextInt(LOWER.length)])
            }
            if (includeDigits) {
                pool.append(DIGITS)
                guaranteed.add(DIGITS[secureRandom.nextInt(DIGITS.length)])
            }
            if (includeSymbols) {
                pool.append(SYMBOLS)
                guaranteed.add(SYMBOLS[secureRandom.nextInt(SYMBOLS.length)])
            }
        }

        if (pool.isEmpty()) {
            _uiState.update { it.copy(generatedPassword = "", strengthScore = 0f, strengthLabel = "") }
            return
        }

        val chars = guaranteed.toMutableList()
        val poolStr = pool.toString()
        val remaining = (state.length - guaranteed.size).coerceAtLeast(0)
        repeat(remaining) {
            chars.add(poolStr[secureRandom.nextInt(poolStr.length)])
        }
        chars.shuffle(kotlinRandom)

        val password = chars.joinToString("")
        val poolSize = poolStr.toSet().size
        val entropy = state.length * log2(poolSize.toDouble().coerceAtLeast(1.0))

        val (score, label) = when {
            entropy < 45.0 -> 0.25f to "Weak"
            entropy < 65.0 -> 0.50f to "Fair"
            entropy < 85.0 -> 0.75f to "Strong"
            else -> 1.0f to "Very Strong"
        }

        _uiState.update {
            it.copy(
                generatedPassword = password,
                strengthScore = score,
                strengthLabel = label,
            )
        }
    }
}
