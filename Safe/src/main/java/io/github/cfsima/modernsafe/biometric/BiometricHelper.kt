package io.github.cfsima.modernsafe.biometric

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import io.github.cfsima.modernsafe.R
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object BiometricHelper {
    private const val TAG = "BiometricHelper"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "modernsafe_biometric_key"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            val biometricManager = context.getSystemService(BiometricManager::class.java) ?: return false
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "isBiometricAvailable error", e)
            false
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun deleteKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete biometric key", e)
        }
    }

    fun initEncryptCipher(): Cipher? {
        return try {
            deleteKey()
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init encrypt cipher", e)
            null
        }
    }

    fun initDecryptCipher(payload: String): Cipher? {
        return try {
            val combined = Base64.decode(payload, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH + 16) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return null

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Biometric key permanently invalidated (new biometric enrolled)", e)
            deleteKey()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init decrypt cipher", e)
            null
        }
    }

    fun encryptMasterKey(cipher: Cipher, masterKey: String): String {
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(masterKey.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptMasterKey(cipher: Cipher, payload: String): String {
        val combined = Base64.decode(payload, Base64.NO_WRAP)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    fun showBiometricPrompt(
        activity: Activity,
        cipher: Cipher,
        title: String = activity.getString(R.string.biometric_prompt_title),
        subtitle: String = activity.getString(R.string.biometric_prompt_subtitle),
        negativeButtonText: String = activity.getString(R.string.use_password),
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(negativeButtonText, activity.mainExecutor) { _, _ -> onCancel() }
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        val cancellationSignal = CancellationSignal()

        prompt.authenticate(
            cryptoObject,
            cancellationSignal,
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authCipher = result.cryptoObject?.cipher
                    if (authCipher != null) {
                        onSuccess(authCipher)
                    } else {
                        onError("Biometric authentication failed: missing cipher")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON
                    ) {
                        onCancel()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // Non-fatal: user can retry
                }
            }
        )
    }
}
