package com.example.sleepcycle.model

import java.time.LocalTime

/**
 * 睡眠周期计算核心引擎
 *
 * 理论依据：
 * - 一个完整的睡眠周期为 90 分钟 (REM + NREM)
 * - 正常成年人平均入睡潜伏期为 14 分钟
 */
object SleepCalculator {
    const val CYCLE_MINUTES = 90
    const val DEFAULT_FALL_ASLEEP_LATENCY_MINUTES = 14
    const val RECOMMENDED_CYCLE_COUNT = 5

    /**
     * 根据入睡时间正推起床时间列表 (推荐 1~6 个周期)
     *
     * 实际入睡点 = bedtime + latencyMinutes
     * 起床时间 = 实际入睡点 + cycleCount * 90min
     *
     * @param bedtime 准备上床/入睡时间
     * @param latencyMinutes 入睡潜伏期缓冲时间 (默认 14 分钟)
     * @param cycleRange 周期范围 (默认 1..6)
     */
    fun calculateWakeUpTimes(
        bedtime: LocalTime,
        latencyMinutes: Int = DEFAULT_FALL_ASLEEP_LATENCY_MINUTES,
        cycleRange: IntRange = 1..6
    ): List<SleepRecommendation> {
        val actualSleepStartTime = bedtime.plusMinutes(latencyMinutes.toLong())
        return cycleRange.map { cycles ->
            val sleepDurationMinutes = cycles * CYCLE_MINUTES
            val wakeTime = actualSleepStartTime.plusMinutes(sleepDurationMinutes.toLong())
            SleepRecommendation(
                cycleCount = cycles,
                targetTime = wakeTime,
                totalMinutes = sleepDurationMinutes,
                quality = evaluateQuality(cycles),
                isRecommended = (cycles == RECOMMENDED_CYCLE_COUNT)
            )
        }
    }

    /**
     * 根据期望起床时间倒推上床睡觉时间列表 (推荐 1~6 个周期)
     *
     * 实际入睡点 = wakeTime - cycleCount * 90min
     * 上床时间 = 实际入睡点 - latencyMinutes
     *
     * @param wakeTime 期望起床时间
     * @param latencyMinutes 入睡潜伏期缓冲时间 (默认 14 分钟)
     * @param cycleRange 周期范围 (默认 1..6)
     */
    fun calculateBedtimes(
        wakeTime: LocalTime,
        latencyMinutes: Int = DEFAULT_FALL_ASLEEP_LATENCY_MINUTES,
        cycleRange: IntRange = 1..6
    ): List<SleepRecommendation> {
        return cycleRange.map { cycles ->
            val sleepDurationMinutes = cycles * CYCLE_MINUTES
            // 倒推入睡时间：先减去纯睡眠周期时长，再减去入睡潜伏期
            val bedtime = wakeTime
                .minusMinutes(sleepDurationMinutes.toLong())
                .minusMinutes(latencyMinutes.toLong())
            SleepRecommendation(
                cycleCount = cycles,
                targetTime = bedtime,
                totalMinutes = sleepDurationMinutes,
                quality = evaluateQuality(cycles),
                isRecommended = (cycles == RECOMMENDED_CYCLE_COUNT)
            )
        }
    }

    /**
     * 「我现在就睡」模式计算推荐起床时间
     *
     * @param currentTime 当前时间
     * @param latencyMinutes 入睡潜伏期缓冲时间 (默认 14 分钟)
     */
    fun calculateSleepNowWakeUpTimes(
        currentTime: LocalTime,
        latencyMinutes: Int = DEFAULT_FALL_ASLEEP_LATENCY_MINUTES
    ): List<SleepRecommendation> {
        return calculateWakeUpTimes(
            bedtime = currentTime,
            latencyMinutes = latencyMinutes
        )
    }

    private fun evaluateQuality(cycles: Int): SleepQuality {
        return when (cycles) {
            6 -> SleepQuality.EXCELLENT
            5 -> SleepQuality.EXCELLENT
            4 -> SleepQuality.OPTIMAL
            3 -> SleepQuality.SUFFICIENT
            2 -> SleepQuality.RECHARGE
            1 -> SleepQuality.NAP
            else -> SleepQuality.SHORT
        }
    }
}
