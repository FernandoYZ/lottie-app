package com.fernandoyz.lottie.data.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException

/** In-memory DAO for JVM tests. SQL behaviour is covered by instrumented tests. */
class FakeMenstruationRecordDao(var failWrites: Boolean = false) : MenstruationRecordDao {

    private val rows = MutableStateFlow<List<MenstruationRecordEntity>>(emptyList())
    private var nextId = 1L

    val all: List<MenstruationRecordEntity> get() = rows.value.sortedByDescending { it.startEpochDay }

    override fun observeAll(): Flow<List<MenstruationRecordEntity>> =
        rows.map { list -> list.sortedByDescending { it.startEpochDay } }

    override suspend fun getAll(): List<MenstruationRecordEntity> = all

    override suspend fun getById(id: Long): MenstruationRecordEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(record: MenstruationRecordEntity): Long {
        if (failWrites) throw IOException("Simulated write failure")
        val id = nextId++
        rows.value = rows.value + record.copy(id = id)
        return id
    }

    override suspend fun update(record: MenstruationRecordEntity) {
        if (failWrites) throw IOException("Simulated write failure")
        rows.value = rows.value.map { if (it.id == record.id) record else it }
    }

    override suspend fun deleteById(id: Long) {
        if (failWrites) throw IOException("Simulated write failure")
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        if (failWrites) throw IOException("Simulated write failure")
        rows.value = emptyList()
    }

    override suspend fun insertAll(records: List<MenstruationRecordEntity>) {
        if (failWrites) throw IOException("Simulated write failure")
        rows.value = rows.value + records.map { it.copy(id = nextId++) }
    }

    override suspend fun replaceAll(records: List<MenstruationRecordEntity>) {
        if (failWrites) throw IOException("Simulated write failure")
        rows.value = records.map { it.copy(id = nextId++) }
    }
}
