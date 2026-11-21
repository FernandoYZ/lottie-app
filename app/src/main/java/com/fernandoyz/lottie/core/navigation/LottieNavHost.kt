package com.fernandoyz.lottie.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fernandoyz.lottie.R
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.reminders.ReminderRepository
import com.fernandoyz.lottie.domain.ReminderSettings
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.privacy.DataEraser
import com.fernandoyz.lottie.data.privacy.PrivacyRepository
import com.fernandoyz.lottie.security.DeviceAuthenticator
import com.fernandoyz.lottie.presentation.dailylog.DailyLogRoute
import com.fernandoyz.lottie.presentation.dailylog.DailyLogViewModel
import java.time.LocalDate
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.presentation.calendar.CalendarRoute
import com.fernandoyz.lottie.presentation.calendar.CalendarViewModel
import com.fernandoyz.lottie.presentation.history.HistoryRoute
import com.fernandoyz.lottie.presentation.history.HistoryViewModel
import com.fernandoyz.lottie.presentation.home.HomeRoute
import com.fernandoyz.lottie.presentation.onboarding.setup.SetupRoute
import com.fernandoyz.lottie.presentation.onboarding.setup.SetupViewModel
import com.fernandoyz.lottie.presentation.onboarding.welcome.WelcomeScreen
import com.fernandoyz.lottie.presentation.settings.SettingsRoute
import com.fernandoyz.lottie.presentation.settings.SettingsViewModel
import java.time.Clock

/** Route constants (NAVIGATION.md). */
object LottieDestinations {
    const val WELCOME = "welcome"
    const val INITIAL_SETUP = "initial_setup"
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val CALENDAR_EDIT = "edit"
    /** Calendar tab; with edit=true it opens in edit mode (HOME §7.1). */
    const val CALENDAR_ROUTE = "calendar?$CALENDAR_EDIT={$CALENDAR_EDIT}"
    const val CALENDAR_EDITING = "calendar?$CALENDAR_EDIT=true"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DAILY_LOG_EPOCH_DAY = "epochDay"
    const val DAILY_LOG = "daily_log/{$DAILY_LOG_EPOCH_DAY}"

    fun dailyLog(date: LocalDate) = "daily_log/${date.toEpochDay()}"
}

/** Top-level tabs of the bottom bar (bottom-bar S1). Welcome and setup stay outside it. */
private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home(LottieDestinations.HOME, R.string.home_title, Icons.Filled.Home),
    Calendar(LottieDestinations.CALENDAR, R.string.calendar_title, Icons.Filled.DateRange),

    History(LottieDestinations.HISTORY, R.string.history_section, Icons.AutoMirrored.Filled.List),
    Settings(LottieDestinations.SETTINGS, R.string.settings_title, Icons.Filled.Settings),
}

@Composable
fun LottieNavHost(
    startDestination: String,
    setupRepository: SetupRepository,
    historyRepository: HistoryRepository,
    dailyLogRepository: DailyLogRepository,
    reminderRepository: ReminderRepository,
    onRemindersChanged: (ReminderSettings) -> Unit,
    privacyRepository: PrivacyRepository,
    dataEraser: DataEraser,
    onDataErased: () -> Unit,
    authenticator: DeviceAuthenticator?,
    clock: Clock,
    modifier: Modifier = Modifier,
    openDailyLogToday: Boolean = false,
) {
    val navController = rememberNavController()
    // The daily log reminder opens today's log over Home (REMINDERS §1), once: the app lock
    // (SECURITY §1) removes and restores this composable, and that must not open it again.
    var openedDailyLog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (openDailyLogToday && !openedDailyLog) {
            openedDailyLog = true
            navController.navigate(LottieDestinations.dailyLog(LocalDate.now(clock)))
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Without query arguments, so "calendar?edit=..." still selects the Calendar tab.
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')

    // The outer Scaffold in LottieApp already applies the system insets.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (Tab.entries.any { it.route == currentRoute }) BottomBar(navController, currentRoute)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LottieDestinations.WELCOME) {
                WelcomeScreen(onStart = { navController.navigate(LottieDestinations.INITIAL_SETUP) })
            }
            composable(LottieDestinations.INITIAL_SETUP) {
                SetupRoute(
                    viewModel = viewModel(factory = SetupViewModel.factory(historyRepository, clock)),
                    onSetupSaved = {
                        // Clear the onboarding from the back stack so Back does not return to it.
                        navController.navigate(LottieDestinations.HOME) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable(LottieDestinations.HOME) {
                HomeRoute(
                    viewModel = viewModel(factory = HistoryViewModel.factory(historyRepository, setupRepository, dailyLogRepository, clock)),
                    onOpenDailyLog = { date -> navController.navigate(LottieDestinations.dailyLog(date)) },
                    onEditMenstruations = {
                        navController.navigate(LottieDestinations.CALENDAR_EDITING) {
                            popUpTo(LottieDestinations.HOME) { saveState = true }
                            launchSingleTop = true
                            // No restoreState: a restored calendar would keep its old, non-editing arguments.
                        }
                    },
                )
            }
            composable(
                route = LottieDestinations.DAILY_LOG,
                arguments = listOf(navArgument(LottieDestinations.DAILY_LOG_EPOCH_DAY) { type = NavType.LongType }),
            ) { entry ->
                val date = LocalDate.ofEpochDay(entry.arguments?.getLong(LottieDestinations.DAILY_LOG_EPOCH_DAY) ?: 0L)
                DailyLogRoute(
                    viewModel = viewModel(
                        factory = DailyLogViewModel.factory(dailyLogRepository, historyRepository, setupRepository, clock, date),
                    ),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = LottieDestinations.CALENDAR_ROUTE,
                arguments = listOf(navArgument(LottieDestinations.CALENDAR_EDIT) { type = NavType.BoolType; defaultValue = false }),
            ) { entry ->
                CalendarRoute(
                    startEditing = entry.arguments?.getBoolean(LottieDestinations.CALENDAR_EDIT) == true,
                    viewModel = viewModel(factory = CalendarViewModel.factory(historyRepository, setupRepository, dailyLogRepository, clock)),
                    onOpenDailyLog = { date -> navController.navigate(LottieDestinations.dailyLog(date)) },
                )
            }
            composable(LottieDestinations.HISTORY) {
                HistoryRoute(viewModel = viewModel(factory = HistoryViewModel.factory(historyRepository, setupRepository, dailyLogRepository, clock)))
            }
            composable(LottieDestinations.SETTINGS) {
                SettingsRoute(
                    authenticator = authenticator,
                    onDataErased = onDataErased,
                    viewModel = viewModel(
                        factory = SettingsViewModel.factory(
                            setupRepository,
                            historyRepository,
                            reminderRepository,
                            privacyRepository,
                            dataEraser,
                            onRemindersChanged,
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavController, currentRoute: String?) {
    NavigationBar(windowInsets = WindowInsets(0)) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == currentRoute,
                onClick = {
                    navController.navigate(tab.route) {
                        // Home is always the root after the onboarding: keep one copy of each tab.
                        popUpTo(LottieDestinations.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}
