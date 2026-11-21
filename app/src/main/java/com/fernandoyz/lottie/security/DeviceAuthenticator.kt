package com.fernandoyz.lottie.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Asks for the phone's own lock: fingerprint, face, or PIN, pattern or password (SECURITY §1).
 * Lottie never sees or stores any of them. BIOMETRIC_WEAK | DEVICE_CREDENTIAL works on API 26+.
 */
class DeviceAuthenticator(
    private val activity: FragmentActivity,
    private val appLock: AppLock,
    private val title: String,
) {

    /** False when the phone has no screen lock configured. */
    fun isAvailable(): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** [onResult] gets true once confirmed, false when cancelled or failed for good. */
    fun authenticate(onResult: (Boolean) -> Unit) {
        appLock.onAuthenticationStarted()
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    appLock.onAuthenticationFinished(success = true)
                    onResult(true)
                }

                // A wrong finger only shows a hint in the prompt; an error closes it.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    appLock.onAuthenticationFinished(success = false)
                    onResult(false)
                }
            },
        )
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setAllowedAuthenticators(AUTHENTICATORS).build())
    }

    private companion object {
        const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}
