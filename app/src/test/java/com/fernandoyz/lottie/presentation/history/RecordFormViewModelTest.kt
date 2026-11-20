package com.fernandoyz.lottie.presentation.history

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Synthetic dates only; never real user data. */
class RecordFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 6, 15)
    private val zone = ZoneId.of("America/Lima")
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)

    private val dao = FakeMenstruationRecordDao()
    private val repository = HistoryRepository(dao, SetupRepository(InMemoryDataStore()))

    private fun newForm() = RecordFormViewModel(repository, clock, recordId = null)

    @Test
    fun `new record cannot be saved without start date`() {
        val state = newForm().uiState.value

        assertFalse(state.isEditing)
        assertFalse(state.canSave)
    }

    @Test
    fun `valid new record is saved`() = runTest {
        val vm = newForm()

        vm.onStartSelected(LocalDate.of(2026, 5, 1))
        vm.onEndSelected(LocalDate.of(2026, 5, 4))
        vm.onSave()

        assertTrue(vm.uiState.value.isDone)
        assertEquals(
            listOf(LocalDate.of(2026, 5, 1) to LocalDate.of(2026, 5, 4)),
            repository.records.first().map { it.startDate to it.endDate },
        )
    }

    @Test
    fun `new record without end date can be saved`() = runTest {
        val vm = newForm()

        vm.onStartSelected(LocalDate.of(2026, 6, 14))
        vm.onSave()

        assertTrue(vm.uiState.value.isDone)
        assertNull(repository.records.first().single().endDate)
    }

    @Test
    fun `broken rule is shown and blocks saving`() = runTest {
        repository.add(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), today)
        val vm = newForm()

        vm.onStartSelected(LocalDate.of(2026, 5, 3))
        vm.onEndSelected(LocalDate.of(2026, 5, 6))

        assertEquals(HistoryValidationError.Overlap, vm.uiState.value.validationError)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `editing loads the record and saves an end date`() = runTest {
        repository.add(LocalDate.of(2026, 6, 10), null, today)
        val id = repository.records.first().single().id
        val vm = RecordFormViewModel(repository, clock, recordId = id)

        assertTrue(vm.uiState.value.isEditing)
        assertEquals(LocalDate.of(2026, 6, 10), vm.uiState.value.startDate)

        vm.onEndSelected(LocalDate.of(2026, 6, 13))
        vm.onSave()

        assertTrue(vm.uiState.value.isDone)
        assertEquals(MenstruationRecord(id, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 13)), repository.getById(id))
    }

    @Test
    fun `end date can be cleared on the latest record`() = runTest {
        repository.add(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 12), today)
        val id = repository.records.first().single().id
        val vm = RecordFormViewModel(repository, clock, recordId = id)

        vm.onEndCleared()
        vm.onSave()

        assertNull(repository.getById(id)?.endDate)
    }

    @Test
    fun `delete removes the record`() = runTest {
        repository.add(LocalDate.of(2026, 6, 10), null, today)
        val id = repository.records.first().single().id
        val vm = RecordFormViewModel(repository, clock, recordId = id)

        vm.onDelete()

        assertTrue(vm.uiState.value.isDone)
        assertTrue(repository.records.first().isEmpty())
    }

    @Test
    fun `save failure is reported and not shown as done`() = runTest {
        val vm = newForm()
        vm.onStartSelected(LocalDate.of(2026, 5, 1))
        dao.failWrites = true

        vm.onSave()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isDone)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `delete failure is reported and not shown as done`() = runTest {
        repository.add(LocalDate.of(2026, 6, 10), null, today)
        val id = repository.records.first().single().id
        val vm = RecordFormViewModel(repository, clock, recordId = id)
        dao.failWrites = true

        vm.onDelete()

        assertTrue(vm.uiState.value.deleteFailed)
        assertFalse(vm.uiState.value.isDone)
    }
}
