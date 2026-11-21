package com.fernandoyz.lottie.security

import org.junit.Assert.assertEquals
import org.junit.Test

/** SECURITY §1: when Lottie asks for the phone lock. */
class AppLockTest {

    private val lock = AppLock()

    @Test
    fun `nothing is shown until the setting is known`() {
        assertEquals(LockState.Loading, lock.state.value)
    }

    @Test
    fun `with the lock off the app opens and never locks`() {
        lock.onSettingLoaded(false)
        lock.onBackground()

        assertEquals(LockState.Unlocked, lock.state.value)
    }

    @Test
    fun `with the lock on the app opens locked and unlocks after confirming`() {
        lock.onSettingLoaded(true)
        assertEquals(LockState.Locked, lock.state.value)

        lock.onAuthenticationStarted()
        lock.onAuthenticationFinished(success = false)
        assertEquals(LockState.Locked, lock.state.value)

        lock.onAuthenticationStarted()
        lock.onAuthenticationFinished(success = true)
        assertEquals(LockState.Unlocked, lock.state.value)
    }

    @Test
    fun `going to the background locks again`() {
        lock.onSettingLoaded(true)
        lock.onAuthenticationStarted()
        lock.onAuthenticationFinished(success = true)

        lock.onBackground()

        assertEquals(LockState.Locked, lock.state.value)
    }

    @Test
    fun `the phone's own lock screen does not count as leaving the app`() {
        lock.onSettingLoaded(true)
        lock.onAuthenticationStarted()
        lock.onBackground() // the PIN screen covers Lottie
        lock.onAuthenticationFinished(success = true)

        assertEquals(LockState.Unlocked, lock.state.value)
    }

    @Test
    fun `turning the lock on from settings keeps the open app unlocked`() {
        lock.onSettingLoaded(false)
        lock.onSettingLoaded(true)

        assertEquals(LockState.Unlocked, lock.state.value)
        lock.onBackground()
        assertEquals(LockState.Locked, lock.state.value)
    }

    @Test
    fun `turning the lock off unlocks`() {
        lock.onSettingLoaded(true)
        lock.onSettingLoaded(false)

        assertEquals(LockState.Unlocked, lock.state.value)
    }
}
