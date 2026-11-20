package com.fernandoyz.lottie.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

/** DataStore whose reads and/or writes fail, to verify error handling. */
class FailingDataStore(
    private val failReads: Boolean = false,
    private val failWrites: Boolean = true,
) : DataStore<Preferences> {

    override val data: Flow<Preferences>
        get() = if (failReads) flow { throw IOException("Simulated read failure") } else flowOf(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        if (failWrites) throw IOException("Simulated write failure")
        return transform(emptyPreferences())
    }
}
