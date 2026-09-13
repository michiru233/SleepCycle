package com.example.sleepcycle.model

import java.time.LocalDate
import java.time.LocalTime

/** 达标日四档（见 CONTEXT.md）：趋势柱状图与热力图按此着色 */
enum class SleepGoalLevel { ON_TARGET, NEAR_TARGET, LARGE_GAP, NO_RECORD }

/** 逐日可视化统计：半成品与无记录日 primarySleepMinutes 为 null（空档） */
data class DailySleepStat(
    val date: LocalDate,
    val primarySleepMinutes: Int?,
    val bedtime: LocalTime?,
    val wakeTime: LocalTime?,
    val napMinutes: Int,
    val goalLevel: SleepGoalLevel
)

object SleepVisualizationCalculator {
    const val NEAR_TARGET_MAX_GAP_MINUTES = 60

    /**
     * 统计窗口（见 CONTEXT.md）内的逐日序列，含今天（睡眠日=醒来日，昨晚睡眠记在今天）。
     * 口径与既有统计一致：齐全记录才计入，半成品与无记录日为 NO_RECORD 空档。
     */
    fun dailyStats(
        records: List<SleepRecord>,
        windowDays: Int,
        targetMinutes: Int,
        today: LocalDate = LocalDate.now()
    ): List<DailySleepStat> {
        val byDate = records.associateBy { it.date }
        return (windowDays - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val record = byDate[date]
            if (record == null || !record.isComplete) {
                DailySleepStat(date, null, null, null, record?.napMinutes ?: 0, SleepGoalLevel.NO_RECORD)
            } else {
                val primary = record.effectivePrimarySleepMinutes ?: 0
                val level = when {
                    primary >= targetMinutes -> SleepGoalLevel.ON_TARGET
                    targetMinutes - primary <= NEAR_TARGET_MAX_GAP_MINUTES -> SleepGoalLevel.NEAR_TARGET
                    else -> SleepGoalLevel.LARGE_GAP
                }
                DailySleepStat(date, primary, record.bedtime, record.wakeTime, record.napMinutes, level)
            }
        }
    }
}
