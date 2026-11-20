package com.fernandoyz.lottie

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.fernandoyz.lottie.data.repository.SetupRepository

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lottie_settings")

/** Manual dependency composition (ARCHITECTURE.md §4). */
class LottieApplication : Application() {
    val setupRepository: SetupRepository by lazy { SetupRepository(settingsDataStore) }
}
