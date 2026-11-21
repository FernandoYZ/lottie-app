package com.fernandoyz.lottie

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.history.LottieDatabase
import com.fernandoyz.lottie.data.repository.SetupRepository

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lottie_settings")

/** Manual dependency composition (ARCHITECTURE.md §4). */
class LottieApplication : Application() {

    val setupRepository: SetupRepository by lazy { SetupRepository(settingsDataStore) }

    private val database: LottieDatabase by lazy {
        Room.databaseBuilder(this, LottieDatabase::class.java, "lottie.db").build()
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(database.menstruationRecordDao(), setupRepository)
    }

    val dailyLogRepository: DailyLogRepository by lazy { DailyLogRepository(database.dailyLogDao()) }
}
