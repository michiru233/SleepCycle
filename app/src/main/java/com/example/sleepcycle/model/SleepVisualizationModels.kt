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

/** 带状图时间轴：start/end 均为"锚点移位空间"的分钟值（end 可能 >1440），横带按 (rel-start)/(end-start) 落位 */
data class SleepBandAxis(val startMinutes: Int, val endMinutes: Int)

private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

/**
 * 跨午夜对齐（工单 #11）：轴起点取窗口内最早入睡时刻（整点对齐），
 * 每条横带从入睡时刻向右延伸时长，保证任何一天的区间不断裂。
 * ponytail: 极端数据（正午前入睡且与晚间入睡混在同一个窗口）会触发正午锚点回退；
 * 若回退后仍超轴（单次睡眠超过 ~12 小时的病态数据）则截断到轴末端。
 */
fun bandAxis(stats: List<DailySleepStat>): SleepBandAxis? {
    val intervals = stats.mapNotNull { stat ->
        val bed = stat.bedtime?.minuteOfDay() ?: return@mapNotNull null
        val wake = stat.wakeTime?.minuteOfDay() ?: return@mapNotNull null
        bed to SleepRecord.durationBetween(stat.bedtime!!, stat.wakeTime!!)
    }
    if (intervals.isEmpty()) return null

    fun anchorAt(axisStart: Int): SleepBandAxis {
        val maxRelEnd = intervals.maxOf { (bed, duration) ->
            (bed - axisStart + 24 * 60) % (24 * 60) + duration
        }
        return SleepBandAxis(axisStart, axisStart + maxRelEnd)
    }

    val minBed = intervals.minOf { it.first }
    val primary = anchorAt(minBed / 60 * 60)
    return if (primary.endMinutes <= 24 * 60) primary else anchorAt(12 * 60)
}

/** 每日横带在轴上的归一化位置（0..1），半成品/无记录日为 null（留空档） */
fun bandPositions(axis: SleepBandAxis, stats: List<DailySleepStat>): List<Pair<Float, Float>?> {
    val span = (axis.endMinutes - axis.startMinutes).coerceAtLeast(1)
    return stats.map { stat ->
        val bed = stat.bedtime?.minuteOfDay() ?: return@map null
        val wake = stat.wakeTime?.minuteOfDay() ?: return@map null
        val relStart = (bed - axis.startMinutes + 24 * 60) % (24 * 60)
        val relEnd = relStart + SleepRecord.durationBetween(stat.bedtime!!, stat.wakeTime!!)
        Pair(relStart / span.toFloat(), relEnd / span.toFloat())
    }
}
