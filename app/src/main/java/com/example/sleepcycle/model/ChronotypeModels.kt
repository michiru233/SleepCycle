package com.example.sleepcycle.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Simplified MCTQ/MEQ-style answers for self-understanding, not diagnosis. */
data class ChronotypeAnswers(
    val workdayBedtime: LocalTime?,
    val workdaySleepTime: LocalTime?,
    val workdayWakeTime: LocalTime?,
    val freeDayBedtime: LocalTime?,
    val freeDaySleepTime: LocalTime?,
    val freeDayWakeTime: LocalTime?,
    val needsAlarm: Boolean?
)

enum class ChronotypeCategory(val label: String) {
    MORNING("偏晨型"),
    INTERMEDIATE("中间型"),
    EVENING("偏夜型"),
    INCOMPLETE("待完善")
}

data class ChronotypeProfile(
    val answers: ChronotypeAnswers,
    val midpointMinutes: Int?,
    val category: ChronotypeCategory,
    val updatedAtEpochMillis: Long
) {
    val midpointText: String
        get() = midpointMinutes?.let { LocalTime.of(it / 60, it % 60).format(TIME_FORMATTER) } ?: "待完善"

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }
}

object ChronotypeCalculator {
    // Local-clock sleep midpoint thresholds. They are intentionally centralized for review and tuning.
    const val MORNING_THRESHOLD_MINUTES = 5 * 60
    const val EVENING_THRESHOLD_MINUTES = 7 * 60

    fun calculate(answers: ChronotypeAnswers, nowMillis: Long = System.currentTimeMillis()): ChronotypeProfile {
        val workMidpoint = sleepMidpoint(answers.workdaySleepTime, answers.workdayWakeTime)
        val freeMidpoint = sleepMidpoint(answers.freeDaySleepTime, answers.freeDayWakeTime)
        val midpoint = if (workMidpoint != null && freeMidpoint != null) {
            averageClockMinutes(workMidpoint, freeMidpoint)
        } else null
        return ChronotypeProfile(
            answers = answers,
            midpointMinutes = midpoint,
            category = midpoint?.let(::categoryFor) ?: ChronotypeCategory.INCOMPLETE,
            updatedAtEpochMillis = nowMillis
        )
    }

    fun sleepMidpoint(sleepTime: LocalTime?, wakeTime: LocalTime?): Int? {
        if (sleepTime == null || wakeTime == null) return null
        val sleep = sleepTime.toSecondOfDay() / 60
        var wake = wakeTime.toSecondOfDay() / 60
        if (wake <= sleep) wake += MINUTES_PER_DAY
        return ((sleep + (wake - sleep) / 2) % MINUTES_PER_DAY)
    }

    fun categoryFor(midpointMinutes: Int): ChronotypeCategory = when {
        midpointMinutes < MORNING_THRESHOLD_MINUTES -> ChronotypeCategory.MORNING
        midpointMinutes >= EVENING_THRESHOLD_MINUTES -> ChronotypeCategory.EVENING
        else -> ChronotypeCategory.INTERMEDIATE
    }

    private fun averageClockMinutes(first: Int, second: Int): Int {
        val secondAdjusted = if (kotlin.math.abs(first - second) > MINUTES_PER_DAY / 2) {
            if (second < first) second + MINUTES_PER_DAY else second - MINUTES_PER_DAY
        } else second
        return ((first + secondAdjusted) / 2).toInt().mod(MINUTES_PER_DAY)
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
