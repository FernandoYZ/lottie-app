package com.fernandoyz.lottie.data.history

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.fernandoyz.lottie.data.dailylog.DailyLogDao
import com.fernandoyz.lottie.data.dailylog.DailyLogEntity

/**
 * Version 2 adds `daily_logs` (DAILY_LOG §6). The automatic migration only creates that table:
 * `menstruation_records` is not touched. Exported schemas live in `app/schemas/`.
 */
@Database(
    entities = [MenstruationRecordEntity::class, DailyLogEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class LottieDatabase : RoomDatabase() {
    abstract fun menstruationRecordDao(): MenstruationRecordDao

    abstract fun dailyLogDao(): DailyLogDao
}
