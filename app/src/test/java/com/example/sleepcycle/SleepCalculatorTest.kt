package com.example.sleepcycle

import com.example.sleepcycle.model.SleepCalculator
import com.example.sleepcycle.model.SleepQuality
import com.example.sleepcycle.model.SleepRecommendation
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
        // 1 周期 (90min = 1h30m)  -> 13:44 (午睡)
        // 2 周期 (180min = 3h00m) -> 15:14 (补能)
        // 3 周期 (270min = 4h30m) -> 16:44
        // 4 周期 (360min = 6h00m) -> 18:14
        // 5 周期 (450min = 7h30m) -> 19:44
        // 6 周期 (540min = 9h00m) -> 21:14
        val bedtime = LocalTime.of(12, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)

        assertEquals(6, results.size)
        assertEquals(LocalTime.of(13, 44), results[0].targetTime)
        assertEquals(LocalTime.of(15, 14), results[1].targetTime)
        assertEquals(LocalTime.of(16, 44), results[2].targetTime)
        assertEquals(LocalTime.of(18, 14), results[3].targetTime)
        assertEquals(LocalTime.of(19, 44), results[4].targetTime)
        assertEquals(LocalTime.of(21, 14), results[5].targetTime)
    }

    @Test
    fun testCalculateWakeUpTimes_CrossDayMidnight() {
        // 跨天测试：23:00 入睡，14 分钟潜伏期 -> 23:14 实际入睡
        // 1 周期 (90min)  -> 00:44 (次日)
        // 2 周期 (180min) -> 02:14 (次日)
        // 3 周期 (270min) -> 03:44 (次日)
        // 4 周期 (360min) -> 05:14 (次日)
        // 5 周期 (450min) -> 06:44 (次日)
        // 6 周期 (540min) -> 08:14 (次日)
        val bedtime = LocalTime.of(23, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)

        assertEquals(6, results.size)
        assertEquals(LocalTime.of(0, 44), results[0].targetTime)
        assertEquals(LocalTime.of(2, 14), results[1].targetTime)
        assertEquals(LocalTime.of(3, 44), results[2].targetTime)
        assertEquals(LocalTime.of(5, 14), results[3].targetTime)
        assertEquals(LocalTime.of(6, 44), results[4].targetTime)
        assertEquals(LocalTime.of(8, 14), results[5].targetTime)
    }

    @Test
    fun testCalculateBedtimes_StandardMorningWakeUp() {
        // 期望早上 07:00 起床
        // 1 周期 (90min = 1h30m)  + 14min = 104min -> 07:00 - 1h44m = 05:16
        // 2 周期 (180min = 3h00m) + 14min = 194min -> 07:00 - 3h14m = 03:46
        // 3 周期 (270min = 4h30m) + 14min = 284min -> 07:00 - 4h44m = 02:16
        // 4 周期 (360min = 6h00m) + 14min = 374min -> 07:00 - 6h14m = 00:46
        // 5 周期 (450min = 7h30m) + 14min = 464min -> 07:00 - 7h44m = 23:16 (前日)
        // 6 周期 (540min = 9h00m) + 14min = 554min -> 07:00 - 9h14m = 21:46 (前日)
        val wakeTime = LocalTime.of(7, 0)
        val results = SleepCalculator.calculateBedtimes(wakeTime)

        assertEquals(6, results.size)
        assertEquals(LocalTime.of(5, 16), results[0].targetTime)
        assertEquals(LocalTime.of(3, 46), results[1].targetTime)
        assertEquals(LocalTime.of(2, 16), results[2].targetTime)
        assertEquals(LocalTime.of(0, 46), results[3].targetTime)
        assertEquals(LocalTime.of(23, 16), results[4].targetTime)
        assertEquals(LocalTime.of(21, 46), results[5].targetTime)
    }

    @Test
    fun testCalculateBedtimes_CrossDayPastMidnightWakeUp() {
        // 期望凌晨 01:30 起床
        // 1 周期 (90min + 14min = 104min = 1h44m) -> 01:30 - 1h44m = 23:46 (前日)
        // 3 周期 (270min + 14min = 284min = 4h44m) -> 01:30 - 4h44m = 20:46 (前日)
        // 5 周期 (450min + 14min = 464min = 7h44m) -> 01:30 - 7h44m = 17:46 (前日)
        val wakeTime = LocalTime.of(1, 30)
        val results = SleepCalculator.calculateBedtimes(wakeTime)

        assertEquals(LocalTime.of(23, 46), results[0].targetTime)
        assertEquals(LocalTime.of(20, 46), results[2].targetTime)
        assertEquals(LocalTime.of(17, 46), results[4].targetTime)
    }

    @Test
    fun testCalculateSleepNowWakeUpTimes() {
        // 现在 22:30 就睡，默认 14 分钟
        // 5 周期推荐：22:30 + 14min + 450min = 06:14 (次日)
        val currentTime = LocalTime.of(22, 30)
        val results = SleepCalculator.calculateSleepNowWakeUpTimes(currentTime)

        assertEquals(6, results.size)
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

        val cycle1 = results.find { it.cycleCount == 1 }!!
        val cycle2 = results.find { it.cycleCount == 2 }!!
        val cycle3 = results.find { it.cycleCount == 3 }!!
        val cycle4 = results.find { it.cycleCount == 4 }!!
        val cycle5 = results.find { it.cycleCount == 5 }!!
        val cycle6 = results.find { it.cycleCount == 6 }!!

        assertFalse(cycle1.isRecommended)
        assertFalse(cycle2.isRecommended)
        assertFalse(cycle3.isRecommended)
        assertFalse(cycle4.isRecommended)
        assertTrue(cycle5.isRecommended)
        assertFalse(cycle6.isRecommended)

        assertEquals(SleepQuality.NAP, cycle1.quality)
        assertEquals(SleepQuality.RECHARGE, cycle2.quality)
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

    @Test
    fun wakeWindowText_standardCase_isTargetMinus15ToPlus15() {
        // 23:00 入睡、默认潜伏期 14 分钟 -> 实际入睡 23:14 + 450min = 06:44
        // 窗口: 06:44 - 15min = 06:29 到 06:44 + 15min = 06:59
        val bedtime = LocalTime.of(23, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)
        val rec5 = results.find { it.cycleCount == 5 }!!

        assertEquals(LocalTime.of(6, 44), rec5.targetTime)
        assertEquals("06:29–06:59", rec5.wakeWindowText)
    }

    @Test
    fun wakeWindowText_customLatency_target0650_is0635To0705() {
        // 23:00 入睡、潜伏期 20 分钟 -> 实际入睡 23:20 + 450min = 06:50
        // 窗口: 06:50 - 15min = 06:35 到 06:50 + 15min = 07:05 (跨小时不跨天)
        val bedtime = LocalTime.of(23, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime, latencyMinutes = 20)
        val rec5 = results.find { it.cycleCount == 5 }!!

        assertEquals(LocalTime.of(6, 50), rec5.targetTime)
        assertEquals("06:35–07:05", rec5.wakeWindowText)
    }

    @Test
    fun wakeWindowText_crossDayNight_boundaryWrapsPastMidnight() {
        // 目标 00:06 ± 15min -> 00:06 - 15min = 23:51 (前一日) 到 00:06 + 15min = 00:21
        val recommendation = SleepRecommendation(
            cycleCount = 1,
            targetTime = LocalTime.of(0, 6),
            totalMinutes = 90,
            quality = SleepQuality.NAP,
            isRecommended = false
        )
        assertEquals("23:51–00:21", recommendation.wakeWindowText)
    }

    @Test
    fun wakeWindowText_doesNotMutateExistingFields() {
        // 验证 wakeWindowText 的加入不影响既有字段与 formattedTargetTime
        val bedtime = LocalTime.of(23, 0)
        val results = SleepCalculator.calculateWakeUpTimes(bedtime)
        val rec5 = results.find { it.cycleCount == 5 }!!

        assertEquals(5, rec5.cycleCount)
        assertTrue(rec5.isRecommended)
        assertEquals(LocalTime.of(6, 44), rec5.targetTime)
        assertEquals("06:44", rec5.formattedTargetTime)
        assertEquals("7小时30分", rec5.totalHoursText)
        assertEquals("06:29–06:59", rec5.wakeWindowText)
    }
}
