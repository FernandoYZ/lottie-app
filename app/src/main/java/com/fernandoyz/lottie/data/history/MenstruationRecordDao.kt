package com.fernandoyz.lottie.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MenstruationRecordDao {

    @Query("SELECT * FROM menstruation_records ORDER BY start_epoch_day DESC")
    fun observeAll(): Flow<List<MenstruationRecordEntity>>

    @Query("SELECT * FROM menstruation_records ORDER BY start_epoch_day DESC")
    suspend fun getAll(): List<MenstruationRecordEntity>

    @Query("SELECT * FROM menstruation_records WHERE id = :id")
    suspend fun getById(id: Long): MenstruationRecordEntity?

    @Insert
    suspend fun insert(record: MenstruationRecordEntity): Long

    @Update
    suspend fun update(record: MenstruationRecordEntity)

    @Query("DELETE FROM menstruation_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM menstruation_records")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(records: List<MenstruationRecordEntity>)

    /** Replaces the whole history in one transaction. Only used before the onboarding is completed. */
    @Transaction
    suspend fun replaceAll(records: List<MenstruationRecordEntity>) {
        deleteAll()
        insertAll(records)
    }
}
