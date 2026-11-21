package com.fernandoyz.lottie.data.dailylog

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs ORDER BY epoch_day")
    fun observeAll(): Flow<List<DailyLogEntity>>

    @Upsert
    suspend fun upsert(log: DailyLogEntity)

    @Query("DELETE FROM daily_logs WHERE epoch_day = :epochDay")
    suspend fun deleteByDay(epochDay: Long)
}
