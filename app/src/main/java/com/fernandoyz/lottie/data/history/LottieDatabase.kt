package com.fernandoyz.lottie.data.history

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.fernandoyz.lottie.data.dailylog.DailyLogDao
import com.fernandoyz.lottie.data.dailylog.DailyLogEntity

/**
 * Version 2 adds `daily_logs` (DAILY_LOG §6). The automatic migration only creates that table:
 * `menstruation_records` is not touched. Version 3 adds nullable columns (and `water_ml` with default 0) to
 * `daily_logs` (DAILY_LOG §8); existing rows keep their values. Exported schemas live in `app/schemas/`.
 */
@Database(
    entities = [MenstruationRecordEntity::class, DailyLogEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class LottieDatabase : RoomDatabase() {
    abstract fun menstruationRecordDao(): MenstruationRecordDao

    abstract fun dailyLogDao(): DailyLogDao
}
