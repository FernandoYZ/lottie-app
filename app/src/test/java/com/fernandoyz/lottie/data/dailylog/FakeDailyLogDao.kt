package com.fernandoyz.lottie.data.dailylog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

/** In-memory DAO for JVM tests. SQL behaviour is covered by instrumented tests. */
class FakeDailyLogDao(var failWrites: Boolean = false) : DailyLogDao {

    val rows = MutableStateFlow<Map<Long, DailyLogEntity>>(emptyMap())

    override fun observeAll(): Flow<List<DailyLogEntity>> = rows.map { it.values.sortedBy { row -> row.epochDay } }

    override suspend fun upsert(log: DailyLogEntity) {
        if (failWrites) throw IOException("Simulated write failure")
        rows.value = rows.value + (log.epochDay to log)
    }

    override suspend fun deleteByDay(epochDay: Long) {
        if (failWrites) throw IOException("Simulated write failure")
        rows.value = rows.value - epochDay
    }
}
