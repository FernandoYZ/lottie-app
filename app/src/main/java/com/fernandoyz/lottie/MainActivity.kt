package com.fernandoyz.lottie

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fernandoyz.lottie.presentation.start.LockScreen
import com.fernandoyz.lottie.presentation.start.LottieApp
import com.fernandoyz.lottie.reminders.ReminderNotifier
import com.fernandoyz.lottie.reminders.ReminderScheduler
import com.fernandoyz.lottie.security.DeviceAuthenticator
import com.fernandoyz.lottie.security.LockState
import com.fernandoyz.lottie.ui.theme.LottieTheme
import kotlinx.coroutines.launch
import java.time.Clock

/** A FragmentActivity because the phone lock prompt (androidx.biometric) needs one (SECURITY §1). */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LottieApplication
        val clock = Clock.systemDefaultZone()
        // Only on a fresh start: after a configuration change the user is already where they want.
        val openDailyLogToday = savedInstanceState == null && intent.getBooleanExtra(ReminderNotifier.EXTRA_OPEN_DAILY_LOG, false)
        val authenticator = DeviceAuthenticator(this, app.appLock, getString(R.string.lock_prompt_title))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                app.privacyRepository.appLockEnabled.collect { enabled ->
                    app.appLock.onSettingLoaded(enabled)
                    // Blank preview in the recent apps while the lock is on; it also blocks screenshots.
                    if (enabled) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            LottieTheme {
                val lockState by app.appLock.state.collectAsStateWithLifecycle()
                // Nothing until the setting is read, so no data flashes before the lock.
                // While locked the app is not composed at all, so no touch, key, focus or screen reader can reach it.
                // The holder keeps its saved state, so unlocking returns to the same screen.
                val appState = rememberSaveableStateHolder()
                when (lockState) {
                    LockState.Loading -> Unit
                    LockState.Locked -> {
                        BackHandler { moveTaskToBack(true) }
                        LockScreen(onUnlock = { authenticator.authenticate {} })
                    }
                    LockState.Unlocked -> appState.SaveableStateProvider("app") {
                        LottieApp(
                            setupRepository = app.setupRepository,
                            historyRepository = app.historyRepository,
                            dailyLogRepository = app.dailyLogRepository,
                            reminderRepository = app.reminderRepository,
                            onRemindersChanged = { app.reminderScheduler.schedule(it, ReminderScheduler.Mode.Replace) },
                            clock = clock,
                            openDailyLogToday = openDailyLogToday,
                            privacyRepository = app.privacyRepository,
                            authenticator = authenticator,
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app locks it; a rotation does not.
        if (!isChangingConfigurations) (application as LottieApplication).appLock.onBackground()
    }
}
