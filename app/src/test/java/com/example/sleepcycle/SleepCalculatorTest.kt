package com.example.sleepcycle

import com.example.sleepcycle.model.SleepCalculator
import com.example.sleepcycle.model.SleepQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SleepCalculatorTest {

    @Test
    fun testConstants() {
        assertEquals("Single cycle must be 90 minutes", 90, SleepCalculator.CYCLE_MINUTES)
        assertEquals("Default latency must be 14 minutes", 14, SleepCalculator.DEFAULT_FALL_ASLEEP_LATENCY_MINUTES)
        assertEquals("Recommended cycle must be 5", 5, SleepCalculator.RECOMMENDED_CYCLE_COUNT)
    }

    @Test
    fun testCalculateWakeUpTimes_StandardNoCrossDay() {
        // 入睡时间 12:00，潜伏期 14 分钟 -> 实际入睡 12:14
        // 3 周期 (270min = 4h30m) -> 16:44
        // 4 周期 (360min = 6h00m) -> 18:14
        // 5 周期 (450min = 7h30m) -> 19:44
        // 6 周期 (540min = 9h00m) -> 21:14
        val bedtime = LocalTime.of(12, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)

        assertEquals(4, results.size)
        assertEquals(LocalTime.of(16, 44), results[0].targetTime)
        assertEquals(LocalTime.of(18, 14), results[1].targetTime)
        assertEquals(LocalTime.of(19, 44), results[2].targetTime)
        assertEquals(LocalTime.of(21, 14), results[3].targetTime)
    }

    @Test
    fun testCalculateWakeUpTimes_CrossDayMidnight() {
        // 跨天测试：23:00 入睡，14 分钟潜伏期 -> 23:14 实际入睡
        // 3 周期 (270min) -> 03:44 (次日)
        // 4 周期 (360min) -> 05:14 (次日)
        // 5 周期 (450min) -> 06:44 (次日)
        // 6 周期 (540min) -> 08:14 (次日)
        val bedtime = LocalTime.of(23, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)

        assertEquals(LocalTime.of(3, 44), results[0].targetTime)
        assertEquals(LocalTime.of(5, 14), results[1].targetTime)
        assertEquals(LocalTime.of(6, 44), results[2].targetTime)
        assertEquals(LocalTime.of(8, 14), results[3].targetTime)
    }

    @Test
    fun testCalculateBedtimes_StandardMorningWakeUp() {
        // 期望早上 07:00 起床
        // 3 周期 (270min = 4h30m) + 14min = 284min -> 07:00 - 4h44m = 02:16
        // 4 周期 (360min = 6h00m) + 14min = 374min -> 07:00 - 6h14m = 00:46
        // 5 周期 (450min = 7h30m) + 14min = 464min -> 07:00 - 7h44m = 23:16 (前日)
        // 6 周期 (540min = 9h00m) + 14min = 554min -> 07:00 - 9h14m = 21:46 (前日)
        val wakeTime = LocalTime.of(7, 0)
        val results = SleepCalculator.calculateBedtimes(wakeTime)

        assertEquals(4, results.size)
        assertEquals(LocalTime.of(2, 16), results[0].targetTime)
        assertEquals(LocalTime.of(0, 46), results[1].targetTime)
        assertEquals(LocalTime.of(23, 16), results[2].targetTime)
        assertEquals(LocalTime.of(21, 46), results[3].targetTime)
    }

    @Test
    fun testCalculateBedtimes_CrossDayPastMidnightWakeUp() {
        // 期望凌晨 01:30 起床
        // 3 周期 (270min + 14min = 284min = 4h44m) -> 01:30 - 4h44m = 20:46 (前日)
        // 5 周期 (450min + 14min = 464min = 7h44m) -> 01:30 - 7h44m = 17:46 (前日)
        val wakeTime = LocalTime.of(1, 30)
        val results = SleepCalculator.calculateBedtimes(wakeTime)

        assertEquals(LocalTime.of(20, 46), results[0].targetTime)
        assertEquals(LocalTime.of(17, 46), results[2].targetTime)
    }

    @Test
    fun testCalculateSleepNowWakeUpTimes() {
        // 现在 22:30 就睡，默认 14 分钟
        // 5 周期推荐：22:30 + 14min + 450min = 06:14 (次日)
        val currentTime = LocalTime.of(22, 30)
        val results = SleepCalculator.calculateSleepNowWakeUpTimes(currentTime)

        assertEquals(4, results.size)
        val rec5 = results.find { it.cycleCount == 5 }
        assertTrue(rec5 != null)
        assertEquals(LocalTime.of(6, 14), rec5!!.targetTime)
        assertTrue(rec5.isRecommended)
    }

    @Test
    fun testCustomLatencyMinutes() {
        // 自定义潜伏期 20 分钟，23:00 入睡
        // 5 周期：23:00 + 20min + 450min = 06:50
        val bedtime = LocalTime.of(23, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime, latencyMinutes = 20)

        val rec5 = results.find { it.cycleCount == 5 }!!
        assertEquals(LocalTime.of(6, 50), rec5.targetTime)
    }

    @Test
    fun testRecommendationAndQualityMapping() {
        val bedtime = LocalTime.of(22, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)

        val cycle3 = results.find { it.cycleCount == 3 }!!
        val cycle4 = results.find { it.cycleCount == 4 }!!
        val cycle5 = results.find { it.cycleCount == 5 }!!
        val cycle6 = results.find { it.cycleCount == 6 }!!

        assertFalse(cycle3.isRecommended)
        assertFalse(cycle4.isRecommended)
        assertTrue(cycle5.isRecommended)
        assertFalse(cycle6.isRecommended)

        assertEquals(SleepQuality.SUFFICIENT, cycle3.quality)
        assertEquals(SleepQuality.OPTIMAL, cycle4.quality)
        assertEquals(SleepQuality.EXCELLENT, cycle5.quality)
        assertEquals(SleepQuality.EXCELLENT, cycle6.quality)
    }

    @Test
    fun testFormattedTexts() {
        val bedtime = LocalTime.of(23, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)
        val rec5 = results.find { it.cycleCount == 5 }!!

        assertEquals("7小时30分", rec5.totalHoursText)
        assertEquals("06:44", rec5.formattedTargetTime)

        val rec4 = results.find { it.cycleCount == 4 }!!
        assertEquals("6小时", rec4.totalHoursText)
    }
}
