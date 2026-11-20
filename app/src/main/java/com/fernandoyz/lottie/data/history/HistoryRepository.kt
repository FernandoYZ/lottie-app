package com.fernandoyz.lottie.data.history

import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.HistoryValidation
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Menstruation history stored in Room (ADR-001). Validates STAGE_2 §3 rules before every write.
 * Write failures are propagated to the caller.
 */
class HistoryRepository(
    private val dao: MenstruationRecordDao,
    private val setupRepository: SetupRepository,
) {

    /** All records, newest first. */
    val records: Flow<List<MenstruationRecord>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): MenstruationRecord? = dao.getById(id)?.toDomain()

    /**
     * Turns the onboarding dates into the first record (STAGE_2 §2); the end date is optional.
     * Runs once: the flag prevents re-creating it if the user deletes it later.
     */
    suspend fun seedInitialRecordIfNeeded() {
        if (setupRepository.isHistorySeeded()) return
        val setup = setupRepository.initialSetup.first() ?: return
        // An existing history means the record was already created before the flag was saved.
        if (dao.getAll().isEmpty()) {
            dao.insert(
                MenstruationRecordEntity(
                    startEpochDay = setup.lastMenstruationDate.toEpochDay(),
                    endEpochDay = setup.lastMenstruationEndDate?.toEpochDay(),
                ),
            )
        }
        setupRepository.markHistorySeeded()
    }

    /** Returns the broken rule, or null when the record was stored. */
    suspend fun add(start: LocalDate, end: LocalDate?, today: LocalDate): HistoryValidationError? {
        HistoryValidation.validate(start, end, currentRecords(), today)?.let { return it }
        dao.insert(MenstruationRecordEntity(startEpochDay = start.toEpochDay(), endEpochDay = end?.toEpochDay()))
        return null
    }

    /** Returns the broken rule, or null when the record was updated. */
    suspend fun update(id: Long, start: LocalDate, end: LocalDate?, today: LocalDate): HistoryValidationError? {
        HistoryValidation.validate(start, end, currentRecords(), today, editingId = id)?.let { return it }
        dao.update(MenstruationRecordEntity(id = id, startEpochDay = start.toEpochDay(), endEpochDay = end?.toEpochDay()))
        return null
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    private suspend fun currentRecords(): List<MenstruationRecord> = dao.getAll().map { it.toDomain() }

    private fun MenstruationRecordEntity.toDomain() = MenstruationRecord(
        id = id,
        startDate = LocalDate.ofEpochDay(startEpochDay),
        endDate = endEpochDay?.let(LocalDate::ofEpochDay),
    )
}
