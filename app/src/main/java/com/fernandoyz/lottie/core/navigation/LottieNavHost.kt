package com.fernandoyz.lottie.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.presentation.calendar.CalendarRoute
import com.fernandoyz.lottie.presentation.calendar.CalendarViewModel
import com.fernandoyz.lottie.presentation.history.HistoryRoute
import com.fernandoyz.lottie.presentation.history.HistoryViewModel
import com.fernandoyz.lottie.presentation.history.RecordFormRoute
import com.fernandoyz.lottie.presentation.history.RecordFormViewModel
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
    const val SETTINGS = "settings"

    const val RECORD_ID_ARG = "recordId"
    const val RECORD_FORM = "record_form?$RECORD_ID_ARG={$RECORD_ID_ARG}"
    private const val NEW_RECORD_ID = -1L

    fun recordForm(recordId: Long? = null) = "record_form?$RECORD_ID_ARG=${recordId ?: NEW_RECORD_ID}"

    fun recordIdFromArg(value: Long): Long? = value.takeIf { it != NEW_RECORD_ID }
}

@Composable
fun LottieNavHost(
    startDestination: String,
    setupRepository: SetupRepository,
    historyRepository: HistoryRepository,
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(LottieDestinations.WELCOME) {
            WelcomeScreen(onStart = { navController.navigate(LottieDestinations.INITIAL_SETUP) })
        }
        composable(LottieDestinations.INITIAL_SETUP) {
            SetupRoute(
                viewModel = viewModel(factory = SetupViewModel.factory(setupRepository, clock)),
                onSetupSaved = {
                    // Clear the onboarding from the back stack so Back does not return to it.
                    navController.navigate(LottieDestinations.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(LottieDestinations.HOME) {
            HistoryRoute(
                viewModel = viewModel(factory = HistoryViewModel.factory(historyRepository, setupRepository, clock)),
                onAddRecord = { navController.navigate(LottieDestinations.recordForm()) },
                onOpenRecord = { navController.navigate(LottieDestinations.recordForm(it)) },
                onOpenCalendar = { navController.navigate(LottieDestinations.CALENDAR) },
                onOpenSettings = { navController.navigate(LottieDestinations.SETTINGS) },
            )
        }
        composable(
            route = LottieDestinations.RECORD_FORM,
            arguments = listOf(
                navArgument(LottieDestinations.RECORD_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val recordId = LottieDestinations.recordIdFromArg(
                entry.arguments?.getLong(LottieDestinations.RECORD_ID_ARG) ?: -1L,
            )
            RecordFormRoute(
                viewModel = viewModel(factory = RecordFormViewModel.factory(historyRepository, clock, recordId)),
                onDone = { navController.popBackStack() },
            )
        }
        composable(LottieDestinations.CALENDAR) {
            CalendarRoute(
                viewModel = viewModel(factory = CalendarViewModel.factory(historyRepository, setupRepository, clock)),
            )
        }
        composable(LottieDestinations.SETTINGS) {
            SettingsRoute(
                viewModel = viewModel(factory = SettingsViewModel.factory(setupRepository, historyRepository)),
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
