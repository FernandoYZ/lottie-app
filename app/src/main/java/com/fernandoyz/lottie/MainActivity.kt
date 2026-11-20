package com.fernandoyz.lottie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fernandoyz.lottie.presentation.start.LottieApp
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.Clock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LottieApplication
        val clock = Clock.systemDefaultZone()
        setContent {
            LottieTheme {
                LottieApp(setupRepository = app.setupRepository, historyRepository = app.historyRepository, clock = clock)
            }
        }
    }
}
