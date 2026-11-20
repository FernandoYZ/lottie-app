package com.fernandoyz.lottie.presentation.start

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class StartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `fresh install needs onboarding`() {
        val vm = StartViewModel(SetupRepository(InMemoryDataStore()))

        assertEquals(StartState.NeedsOnboarding, vm.startState.value)
    }

    @Test
    fun `completed setup skips onboarding`() = runTest {
        val repository = SetupRepository(InMemoryDataStore())
        repository.save(InitialSetup(LocalDate.of(2026, 11, 2), 28))

        val vm = StartViewModel(repository)

        assertEquals(StartState.SetupCompleted, vm.startState.value)
    }

    @Test
    fun `read failure is reported as error`() {
        val vm = StartViewModel(SetupRepository(FailingDataStore(failReads = true)))

        assertEquals(StartState.Error, vm.startState.value)
    }
}
