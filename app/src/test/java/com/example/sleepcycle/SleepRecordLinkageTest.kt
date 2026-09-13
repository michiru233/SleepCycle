package com.example.sleepcycle

import com.example.sleepcycle.data.toEntity
import com.example.sleepcycle.data.toRecord
import com.example.sleepcycle.model.SocialJetLagCalculator
import com.example.sleepcycle.model.SocialJetLagResult
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * 工单 #2（睡眠时间管理联动·数据基座）：
 * 半成品记录合法化（允许明天、允许缺入睡或起床一端）与统计过滤。
 * 词汇见 CONTEXT.md；决策依据 ADR-0001。
 */
class SleepRecordLinkageTest {
    // 校验内部用 LocalDate.now() 判定“明天”，测试同样动态取当天以避免日期敏感
    private val today: LocalDate = LocalDate.now()

    private fun completeRecord(date: LocalDate, primary: Int) =
        SleepRecord(date, LocalTime.of(23, 0), LocalTime.of(7, 0), primary, 0)

    @Test
    fun tomorrowIsAllowedAsSleepDayForAlarmLinkage() {
        val record = SleepRecord(
            date = today.plusDays(1),
            bedtime = null,
            wakeTime = LocalTime.of(7, 0),
            primarySleepMinutes = null
        )
        assertEquals(today.plusDays(1), record.date)
        assertFalse(record.isComplete)
    }

    @Test
    fun dayAfterTomorrowIsStillRejected() {
        val error = runCatching {
            SleepRecord(today.plusDays(2), null, LocalTime.of(7, 0), null)
        }.exceptionOrNull()
        assertTrue("超过明天的日期应被拒绝", error is IllegalArgumentException)
    }

    @Test
    fun halfRecordWithWakeOnlyCarriesNoDuration() {
        val record = SleepRecord(today, null, LocalTime.of(7, 0), null)
        assertNull(record.clockDurationMinutes)
        assertNull(record.midpointMinutes)
        assertFalse(record.isComplete)
    }

    @Test
    fun halfRecordWithBedtimeOnlyCarriesNoDuration() {
        val record = SleepRecord(today, LocalTime.of(23, 0), null, null)
        assertNull(record.clockDurationMinutes)
        assertFalse(record.isComplete)
    }

    @Test
    fun halfRecordRoundTripsThroughEntityWithoutSchemaChange() {
        val record = SleepRecord(today.plusDays(1), null, LocalTime.of(7, 0), null, napMinutes = 0)
        assertEquals(record, record.toEntity().toRecord())
        val napOnly = SleepRecord(today, null, null, null, napMinutes = 20)
        assertEquals(napOnly, napOnly.toEntity().toRecord())
    }

    @Test
    fun completeRecordStillRoundTripsAndComputesDuration() {
        val record = SleepRecord(today.minusDays(1), LocalTime.of(23, 30), LocalTime.of(7, 0), 450, 20)
        assertEquals(record, record.toEntity().toRecord())
        assertEquals(450, record.clockDurationMinutes)
        assertEquals(195, record.midpointMinutes)
    }

    @Test
    fun statsExcludeHalfRecordsSoLinkageDoesNotPolluteAverages() {
        val complete = completeRecord(today.minusDays(1), primary = 420)
        val wakeOnly = SleepRecord(today.minusDays(2), null, LocalTime.of(7, 0), null)
        val napOnly = SleepRecord(today.minusDays(3), null, null, null, napMinutes = 20)
        val summary = SleepStatsCalculator.summarize(listOf(complete, wakeOnly, napOnly), 480, today)
        assertEquals(1, summary.recordedDays)
        assertEquals(420, summary.averagePrimarySleepMinutes)
        assertEquals(60, summary.estimatedGapMinutes)
    }

    @Test
    fun socialJetLagIgnoresHalfRecords() {
        val completeWorkday = SleepRecord(today.minusDays(2), LocalTime.of(23, 0), LocalTime.of(7, 0), 480)
        val halfRecord = SleepRecord(today.minusDays(1), null, LocalTime.of(6, 0), null)
        val without = SocialJetLagCalculator.calculate(listOf(completeWorkday), today)
        val with = SocialJetLagCalculator.calculate(listOf(completeWorkday, halfRecord), today)
        assertTrue(without is SocialJetLagResult.Incomplete)
        assertTrue("半成品记录不应改变社交时差结果", with == without)
    }
}
