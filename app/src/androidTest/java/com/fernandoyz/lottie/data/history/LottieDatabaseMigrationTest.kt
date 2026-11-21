package com.fernandoyz.lottie.data.history

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fernandoyz.lottie.data.dailylog.DailyLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** DAILY_LOG §6: version 1 → 2 adds `daily_logs` and leaves the history untouched. Synthetic data only. */
@RunWith(AndroidJUnit4::class)
class LottieDatabaseMigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LottieDatabase::class.java)

    @Test
    fun migrationFrom1To2KeepsMenstruationsAndAddsDailyLogs() = runBlocking {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO menstruation_records (start_epoch_day, end_epoch_day) VALUES (100, 104)")
            execSQL("INSERT INTO menstruation_records (start_epoch_day, end_epoch_day) VALUES (130, NULL)")
            close()
        }

        // Validates the migrated schema against the exported 2.json.
        helper.runMigrationsAndValidate(dbName, 2, true).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            LottieDatabase::class.java,
            dbName,
        ).build()
        try {
            val records = database.menstruationRecordDao().getAll().map { it.startEpochDay to it.endEpochDay }
            assertEquals(listOf(130L to null, 100L to 104L), records)

            val log = DailyLogEntity(120, "Light", true, "Calm", "", "", "", "", null, "")
            database.dailyLogDao().upsert(log)
            assertEquals(listOf(log), database.dailyLogDao().observeAll().first())
        } finally {
            database.close()
        }
    }

    /** DAILY_LOG §8: version 2 → 3 adds empty columns and keeps every existing log and menstruation. */
    @Test
    fun migrationFrom2To3KeepsLogsAndMenstruations() = runBlocking {
        helper.createDatabase(dbName, 2).apply {
            execSQL("INSERT INTO menstruation_records (start_epoch_day, end_epoch_day) VALUES (100, 104)")
            execSQL(
                "INSERT INTO daily_logs (epoch_day, menstrual_flow, clots, moods, symptoms, discharge, digestion, sex, sex_drive, note) " +
                    "VALUES (120, 'Light', 1, 'Calm', '', '', '', '', NULL, 'nota')",
            )
            close()
        }

        // Validates the migrated schema against the exported 3.json.
        helper.runMigrationsAndValidate(dbName, 3, true).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            LottieDatabase::class.java,
            dbName,
        ).build()
        try {
            assertEquals(listOf(100L to 104L), database.menstruationRecordDao().getAll().map { it.startEpochDay to it.endEpochDay })
            assertEquals(
                listOf(DailyLogEntity(120, "Light", true, "Calm", "", "", "", "", null, "nota")),
                database.dailyLogDao().observeAll().first(),
            )
        } finally {
            database.close()
        }
    }
}
