package com.fernandoyz.lottie.data.history

import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.InitialSetup
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/** Synthetic dates only; never real user data. */
class HistoryRepositoryTest {

    private val today = LocalDate.of(2026, 6, 15)
    private val setupDate = LocalDate.of(2026, 6, 1)

    private val dao = FakeMenstruationRecordDao()
    private val setupRepository = SetupRepository(InMemoryDataStore())
    private val repository = HistoryRepository(dao, setupRepository)

    private suspend fun completeSetup() = setupRepository.save(InitialSetup(setupDate))

    @Test
    fun `initial record is created from the setup without end date`() = runTest {
        completeSetup()

        repository.seedInitialRecordIfNeeded()

        assertEquals(listOf(MenstruationRecord(1, setupDate, null)), repository.records.first())
    }

    @Test
    fun `initial record is created only once`() = runTest {
        completeSetup()

        repository.seedInitialRecordIfNeeded()
        repository.seedInitialRecordIfNeeded()

        assertEquals(1, repository.records.first().size)
    }

    @Test
    fun `deleted initial record is not created again`() = runTest {
        completeSetup()
        repository.seedInitialRecordIfNeeded()

        repository.delete(repository.records.first().single().id)
        repository.seedInitialRecordIfNeeded()

        assertTrue(repository.records.first().isEmpty())
    }

    @Test
    fun `no initial record without completed setup`() = runTest {
        repository.seedInitialRecordIfNeeded()

        assertTrue(repository.records.first().isEmpty())
    }

    @Test
    fun `valid record is added and listed newest first`() = runTest {
        assertNull(repository.add(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), today))
        assertNull(repository.add(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 6), today))

        val starts = repository.records.first().map { it.startDate }
        assertEquals(listOf(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 3, 1)), starts)
    }

    @Test
    fun `invalid record is rejected and not stored`() = runTest {
        repository.add(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), today)

        val error = repository.add(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 8), today)

        assertEquals(HistoryValidationError.Overlap, error)
        assertEquals(1, repository.records.first().size)
    }

    @Test
    fun `record is updated, including adding an end date`() = runTest {
        repository.add(LocalDate.of(2026, 6, 10), null, today)
        val id = repository.records.first().single().id

        assertNull(repository.update(id, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 13), today))

        assertEquals(MenstruationRecord(id, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 13)), repository.getById(id))
    }

    @Test
    fun `invalid update is rejected and not stored`() = runTest {
        repository.add(LocalDate.of(2026, 6, 10), null, today)
        val id = repository.records.first().single().id

        val error = repository.update(id, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 9), today)

        assertEquals(HistoryValidationError.EndBeforeStart, error)
        assertNull(repository.getById(id)?.endDate)
    }

    @Test(expected = IOException::class)
    fun `write failure is propagated`() = runTest {
        dao.failWrites = true
        repository.add(LocalDate.of(2026, 3, 1), null, today)
    }

    @Test
    fun `initial record keeps the end date from the setup`() = runTest {
        setupRepository.save(InitialSetup(setupDate, setupDate.plusDays(3)))

        repository.seedInitialRecordIfNeeded()

        assertEquals(listOf(MenstruationRecord(1, setupDate, setupDate.plusDays(3))), repository.records.first())
    }

    @Test
    fun `onboarding stores every marked menstruation and completes the setup`() = runTest {
        val april = LocalDate.of(2026, 4, 3) to LocalDate.of(2026, 4, 7)
        val may = LocalDate.of(2026, 5, 2) to LocalDate.of(2026, 5, 6)

        repository.completeOnboarding(listOf(april, may))

        assertEquals(listOf(may.first, april.first), repository.records.first().map { it.startDate })
        assertEquals(InitialSetup(may.first, may.second), setupRepository.initialSetup.first())
        repository.seedInitialRecordIfNeeded() // already seeded: nothing is added
        assertEquals(2, repository.records.first().size)
    }

    @Test
    fun `retrying an interrupted onboarding does not duplicate records`() = runTest {
        val may = LocalDate.of(2026, 5, 2) to LocalDate.of(2026, 5, 6)
        dao.insert(MenstruationRecordEntity(startEpochDay = may.first.toEpochDay(), endEpochDay = may.second.toEpochDay()))

        repository.completeOnboarding(listOf(may))

        assertEquals(1, repository.records.first().size)
    }
}
