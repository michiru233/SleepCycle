package com.example.sleepcycle

import com.example.sleepcycle.model.ChronotypeAnswers
import com.example.sleepcycle.model.ChronotypeCalculator
import com.example.sleepcycle.model.SocialJetLagCalculator
import com.example.sleepcycle.model.SocialJetLagResult
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepStatsCalculator
import com.example.sleepcycle.model.TwoProcessModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SleepAnalysisTest {
    private val today = LocalDate.of(2026, 8, 26)

    private fun record(daysAgo: Long, bedtime: LocalTime, wake: LocalTime, primary: Int, nap: Int = 0) =
        SleepRecord(today.minusDays(daysAgo), bedtime, wake, primary, nap)

    @Test
    fun gapCountsOnlyCompletePreviousFourteenDatesAndNapsReduceGap() {
        val records = listOf(
            record(1, LocalTime.of(23, 0), LocalTime.of(6, 0), 420, 30),
            record(14, LocalTime.of(23, 0), LocalTime.of(6, 0), 420, 0),
            record(15, LocalTime.of(23, 0), LocalTime.of(6, 0), 100, 0)
        )
        val summary = SleepStatsCalculator.summarize(records, 480, today)
        assertEquals(90, summary.estimatedGapMinutes)
        assertEquals(2, summary.recordedDays)
        assertEquals(420, summary.averagePrimarySleepMinutes)
    }

    @Test
    fun socialJetLagUsesCircularAverageAcrossMidnight() {
        val records = listOf(
            SleepRecord(LocalDate.of(2026, 8, 24), LocalTime.of(22, 30), LocalTime.of(6, 30), 480),
            SleepRecord(LocalDate.of(2026, 8, 25), LocalTime.of(22, 0), LocalTime.of(6, 0), 480),
            SleepRecord(LocalDate.of(2026, 8, 22), LocalTime.of(23, 0), LocalTime.of(7, 0), 480),
            SleepRecord(LocalDate.of(2026, 8, 23), LocalTime.of(23, 0), LocalTime.of(7, 0), 480)
        )
        val result = SocialJetLagCalculator.calculate(records, today)
        assertTrue(result is SocialJetLagResult.Complete)
        assertEquals(45, (result as SocialJetLagResult.Complete).differenceMinutes)
        assertEquals(0.75, result.differenceHours, 0.01)
    }

    @Test
    fun circularMeanWrapsAroundMidnight() {
        assertEquals(0, SocialJetLagCalculator.circularMean(listOf(23 * 60 + 30, 30)))
    }

    @Test
    fun socialJetLagReportsIncompleteWhenEitherGroupHasLessThanTwo() {
        val result = SocialJetLagCalculator.calculate(
            listOf(record(1, LocalTime.of(23, 0), LocalTime.of(7, 0), 480))
        )
        assertEquals(SocialJetLagResult.Incomplete, result)
    }

    @Test
    fun twoProcessOutputHasFixedThirtyMinuteDayAndBoundedValues() {
        val points = TwoProcessModel.generate(emptyList(), null, LocalDateTime.of(2026, 8, 26, 23, 30))
        assertEquals(TwoProcessModel.POINTS, points.size)
        assertEquals(0, points.first().offsetMinutes)
        assertEquals(23 * 60 + 30, points.last().offsetMinutes)
        assertTrue(points.all { it.processS in 0.0..1.0 && it.circadianAlertness in 0.0..1.0 && it.sleepTendency in 0.0..1.0 })
        assertTrue(points.all { !it.processS.isNaN() && !it.sleepTendency.isNaN() })
    }

    @Test
    fun processSIncreasesAwakeAndDecreasesAsleep() {
        val awake = generateSequence(0.2) { TwoProcessModel.processSAt(it, asleep = false, minutes = 30) }.take(5).toList()
        val asleep = generateSequence(0.8) { TwoProcessModel.processSAt(it, asleep = true, minutes = 30) }.take(5).toList()
        assertTrue(awake.zipWithNext().all { it.first < it.second })
        assertTrue(asleep.zipWithNext().all { it.first > it.second })
    }

    @Test
    fun processCHasTwentyFourHourPeriodAndChronotypePhaseChangesResult() {
        assertEquals(TwoProcessModel.processC(120, 180), TwoProcessModel.processC(120 + 1440, 180), 0.0001)
        val profile = ChronotypeCalculator.calculate(
            ChronotypeAnswers(null, LocalTime.of(23, 0), LocalTime.of(7, 0), null, LocalTime.of(1, 0), LocalTime.of(9, 0), null), 1L
        )
        val defaultPoints = TwoProcessModel.generate(emptyList(), null, LocalDateTime.of(2026, 8, 26, 12, 0))
        val profilePoints = TwoProcessModel.generate(emptyList(), profile, LocalDateTime.of(2026, 8, 26, 12, 0))
        assertFalse(defaultPoints.map { it.circadianAlertness } == profilePoints.map { it.circadianAlertness })
    }
}
