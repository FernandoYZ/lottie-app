package com.fernandoyz.lottie.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.presentation.home.HomeScreen
import com.fernandoyz.lottie.presentation.onboarding.setup.SetupRoute
import com.fernandoyz.lottie.presentation.onboarding.setup.SetupViewModel
import com.fernandoyz.lottie.presentation.onboarding.welcome.WelcomeScreen
import java.time.Clock

/** Route constants for Stage 1 (NAVIGATION.md). */
object LottieDestinations {
    const val WELCOME = "welcome"
    const val INITIAL_SETUP = "initial_setup"
    const val HOME = "home"
}

@Composable
fun LottieNavHost(
    startDestination: String,
    setupRepository: SetupRepository,
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
            HomeScreen()
        }
    }
}
