package com.fernandoyz.lottie.data.dailylog

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Digestion
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Daily logs stored in Room (DAILY_LOG §6). Write failures are propagated to the caller. */
class DailyLogRepository(private val dao: DailyLogDao) {

    /** Every logged day, keyed by date. */
    val logs: Flow<Map<LocalDate, DailyLog>> =
        dao.observeAll().map { rows -> rows.associate { LocalDate.ofEpochDay(it.epochDay) to it.toDomain() } }

    /** Stores [log], or deletes its day when nothing is marked. */
    suspend fun save(log: DailyLog) {
        if (log.isEmpty) dao.deleteByDay(log.date.toEpochDay()) else dao.upsert(log.toEntity())
    }

    private fun DailyLog.toEntity() = DailyLogEntity(
        epochDay = date.toEpochDay(),
        menstrualFlow = flow?.name,
        clots = clots,
        moods = moods.join(),
        symptoms = symptoms.join(),
        discharge = discharge.join(),
        digestion = digestion.join(),
        sex = sex.join(),
        sexDrive = sexDrive?.name,
        note = note,
    )

    private fun DailyLogEntity.toDomain() = DailyLog(
        date = LocalDate.ofEpochDay(epochDay),
        flow = menstrualFlow.toEnumOrNull<MenstrualFlow>(),
        clots = clots,
        moods = moods.toEnumSet<Mood>(),
        symptoms = symptoms.toEnumSet<Symptom>(),
        discharge = discharge.toEnumSet<Discharge>(),
        digestion = digestion.toEnumSet<Digestion>(),
        sex = sex.toEnumSet<SexActivity>(),
        sexDrive = sexDrive.toEnumOrNull<SexDrive>(),
        note = note,
    )

    private fun Set<Enum<*>>.join(): String = map { it.name }.sorted().joinToString(",")

    /** Unknown names, e.g. from a future version, are ignored instead of failing the whole read. */
    private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

    private inline fun <reified T : Enum<T>> String.toEnumSet(): Set<T> =
        split(",").mapNotNull { it.toEnumOrNull<T>() }.toSet()
}
