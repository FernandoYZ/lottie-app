package com.fernandoyz.lottie.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockState {
    /** The setting is not read yet: nothing is shown, so no data flashes before the lock. */
    Loading,
    Locked,
    Unlocked,
}

/**
 * When Lottie asks for the phone lock (SECURITY §1): on open and on return from the background while the
 * setting is on. The phone's own lock screen covers the app while it is asked, so that does not count as
 * leaving. Lives in the process: a new process starts locked again.
 */
class AppLock {

    private val _state = MutableStateFlow(LockState.Loading)
    val state: StateFlow<LockState> = _state.asStateFlow()

    private var enabled: Boolean? = null
    private var authenticating = false

    fun onSettingLoaded(value: Boolean) {
        val first = enabled == null
        enabled = value
        when {
            !value -> _state.value = LockState.Unlocked
            // Turning it on from Settings keeps the open app unlocked until it leaves.
            first -> _state.value = LockState.Locked
        }
    }

    fun onBackground() {
        if (enabled == true && !authenticating) _state.value = LockState.Locked
    }

    fun onAuthenticationStarted() {
        authenticating = true
    }

    fun onAuthenticationFinished(success: Boolean) {
        authenticating = false
        if (success) _state.value = LockState.Unlocked
    }
}
