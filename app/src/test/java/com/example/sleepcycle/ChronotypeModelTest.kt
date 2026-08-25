package com.example.sleepcycle

import com.example.sleepcycle.model.ChronotypeAnswers
import com.example.sleepcycle.model.ChronotypeCalculator
import com.example.sleepcycle.model.ChronotypeCategory
import com.example.sleepcycle.model.LightGuidance
import com.example.sleepcycle.model.LightGuidanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ChronotypeModelTest {
    @Test
    fun midpointAcrossMidnightIsCorrect() {
        assertEquals(3 * 60 + 30, ChronotypeCalculator.sleepMidpoint(LocalTime.of(23, 30), LocalTime.of(7, 30)))
    }

    @Test
    fun profileAveragesWorkdayAndFreeDayMidpointsAcrossClockBoundary() {
        val profile = ChronotypeCalculator.calculate(
            ChronotypeAnswers(
                workdayBedtime = LocalTime.of(22, 30), workdaySleepTime = LocalTime.of(23, 30), workdayWakeTime = LocalTime.of(7, 30),
                freeDayBedtime = LocalTime.of(1, 0), freeDaySleepTime = LocalTime.of(1, 30), freeDayWakeTime = LocalTime.of(9, 30),
                needsAlarm = false
            ),
            nowMillis = 42L
        )
        assertEquals(4 * 60 + 30, profile.midpointMinutes)
        assertEquals(ChronotypeCategory.MORNING, profile.category)
        assertEquals(42L, profile.updatedAtEpochMillis)
    }

    @Test
    fun missingAnswerIsIncomplete() {
        val profile = ChronotypeCalculator.calculate(
            ChronotypeAnswers(null, LocalTime.of(23, 30), LocalTime.of(7, 30), null, null, null, null)
        )
        assertEquals(null, profile.midpointMinutes)
        assertEquals(ChronotypeCategory.INCOMPLETE, profile.category)
    }

    @Test
    fun categoryThresholdsAreCentralizedAndThreeWay() {
        assertEquals(ChronotypeCategory.MORNING, ChronotypeCalculator.categoryFor(ChronotypeCalculator.MORNING_THRESHOLD_MINUTES - 1))
        assertEquals(ChronotypeCategory.INTERMEDIATE, ChronotypeCalculator.categoryFor(ChronotypeCalculator.MORNING_THRESHOLD_MINUTES))
        assertEquals(ChronotypeCategory.EVENING, ChronotypeCalculator.categoryFor(ChronotypeCalculator.EVENING_THRESHOLD_MINUTES))
    }

    @Test
    fun lightGuidanceCoversCrossDayAndRequiredCopy() {
        val morning = LightGuidanceCalculator.morningLight(LocalTime.of(23, 58))
        assertEquals(LocalTime.of(0, 3), morning.targetTime)
        assertEquals(LocalTime.of(23, 48), morning.windowStart)
        assertEquals(LocalTime.of(0, 18), morning.windowEnd)
        assertTrue(morning.note.contains("户外光"))
        assertTrue(morning.note.contains("10–30 分钟"))

        val sunset = LightGuidanceCalculator.digitalSunset(LocalTime.of(0, 30))
        assertEquals(LocalTime.of(23, 30), sunset.targetTime)
        assertEquals(LocalTime.of(22, 30), sunset.windowStart)
        assertEquals(LocalTime.of(23, 30), sunset.windowEnd)
        assertTrue(sunset.note.contains("60–120 分钟"))
        assertTrue(sunset.note.contains("个体差异"))
    }
}
