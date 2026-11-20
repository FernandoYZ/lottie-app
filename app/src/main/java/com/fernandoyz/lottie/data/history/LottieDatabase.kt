package com.fernandoyz.lottie.data.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MenstruationRecordEntity::class], version = 1, exportSchema = true)
abstract class LottieDatabase : RoomDatabase() {
    abstract fun menstruationRecordDao(): MenstruationRecordDao
}
