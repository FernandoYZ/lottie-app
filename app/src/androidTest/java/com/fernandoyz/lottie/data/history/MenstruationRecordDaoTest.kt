package com.fernandoyz.lottie.data.history

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic epoch days only; never real user data. */
@RunWith(AndroidJUnit4::class)
class MenstruationRecordDaoTest {

    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        LottieDatabase::class.java,
    ).build()
    private val dao = database.menstruationRecordDao()

    @After
    fun tearDown() = database.close()

    @Test
    fun recordsAreListedNewestFirst() = runBlocking {
        dao.insert(MenstruationRecordEntity(startEpochDay = 100, endEpochDay = 104))
        dao.insert(MenstruationRecordEntity(startEpochDay = 130, endEpochDay = null))

        assertEquals(listOf(130L, 100L), dao.observeAll().first().map { it.startEpochDay })
    }

    @Test
    fun updateAndDeleteWork() = runBlocking {
        val id = dao.insert(MenstruationRecordEntity(startEpochDay = 100, endEpochDay = null))

        dao.update(MenstruationRecordEntity(id = id, startEpochDay = 100, endEpochDay = 103))
        assertEquals(103L, dao.getById(id)?.endEpochDay)

        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun replaceAllSwapsTheWholeHistory() = runBlocking {
        dao.insert(MenstruationRecordEntity(startEpochDay = 100, endEpochDay = 104))

        dao.replaceAll(
            listOf(
                MenstruationRecordEntity(startEpochDay = 100, endEpochDay = 103),
                MenstruationRecordEntity(startEpochDay = 130, endEpochDay = 134),
            ),
        )

        assertEquals(listOf(130L to 134L, 100L to 103L), dao.getAll().map { it.startEpochDay to it.endEpochDay })
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateStartIsRejected() = runBlocking {
        dao.insert(MenstruationRecordEntity(startEpochDay = 100, endEpochDay = null))
        dao.insert(MenstruationRecordEntity(startEpochDay = 100, endEpochDay = 102))
        Unit
    }
}
