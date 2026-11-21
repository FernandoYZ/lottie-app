package com.fernandoyz.lottie.data.privacy

import com.fernandoyz.lottie.data.InMemoryDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** SECURITY §1. */
class PrivacyRepositoryTest {

    private val repository = PrivacyRepository(InMemoryDataStore())

    @Test
    fun `the lock is off by default and is stored`() = runTest {
        assertEquals(false, repository.appLockEnabled.first())

        repository.setAppLockEnabled(true)

        assertEquals(true, repository.appLockEnabled.first())
    }
}
