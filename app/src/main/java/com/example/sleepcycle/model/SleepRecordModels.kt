package com.example.sleepcycle.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.roundToInt

const val DEFAULT_SLEEP_TARGET_MINUTES = 480
const val MIN_SLEEP_TARGET_MINUTES = 360
const val MAX_SLEEP_TARGET_MINUTES = 600
const val SLEEP_STATS_DAYS = 14

data class SleepSettings(val targetMinutes: Int = DEFAULT_SLEEP_TARGET_MINUTES) {
    init {
        require(targetMinutes in MIN_SLEEP_TARGET_MINUTES..MAX_SLEEP_TARGET_MINUTES)
        require(targetMinutes % 15 == 0) { "睡眠目标必须按 15 分钟调整" }
    }
}

// 半成品记录（联动先写入一端，见 CONTEXT.md）：bedtime/wakeTime/primarySleepMinutes 可空
data class SleepRecord(
    val date: LocalDate,
    val bedtime: LocalTime?,
    val wakeTime: LocalTime?,
    val primarySleepMinutes: Int?,
    val napMinutes: Int = 0
) {
    init {
        // 联动允许写入次日的睡眠日（晚间设闹钟归次日），最晚到明天
        require(!date.isAfter(LocalDate.now().plusDays(1))) { "记录最晚允许到明天" }
        require(bedtime == null || wakeTime == null || bedtime != wakeTime) { "入睡和起床时间不能相同" }
        require(primarySleepMinutes == null || primarySleepMinutes in 0..SleepRecord.MINUTES_PER_DAY) { "主睡眠分钟数无效" }
        require(napMinutes in 0..SleepRecord.MINUTES_PER_DAY) { "午睡分钟数无效" }
    }

    val isComplete: Boolean
        get() = bedtime != null && wakeTime != null

    // 统计口径的主睡眠时长：优先用户/联动写入值，齐全记录缺值时按时钟区间回退
    val effectivePrimarySleepMinutes: Int?
        get() = primarySleepMinutes ?: clockDurationMinutes

    val clockDurationMinutes: Int?
        get() = if (isComplete) durationBetween(bedtime!!, wakeTime!!) else null

    val midpointMinutes: Int?
        get() = clockDurationMinutes?.let { (bedtime!!.toMinuteOfDay() + it / 2).mod(MINUTES_PER_DAY) }

    val isWeekend: Boolean
        get() = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    companion object {
        const val MINUTES_PER_DAY = 24 * 60

    fun durationBetween(bedtime: LocalTime, wakeTime: LocalTime): Int {
        val difference = wakeTime.toMinuteOfDay() - bedtime.toMinuteOfDay()
        return if (difference <= 0) difference + MINUTES_PER_DAY else difference
    }

    /**
     * 睡眠日归属（见 CONTEXT.md）：凌晨（6 点前，已在睡眠中）设定归当天，其余时段设定归次日。
     * "+15 分钟"入睡写入与起床锚点闹钟写入共用此规则，保证同一晚的两端落在同一条记录上。
     */
    fun sleepDayOf(setAt: LocalTime, date: LocalDate): LocalDate =
        if (setAt.hour < 6) date else date.plusDays(1)
    }
}

data class SleepGapSummary(
    val estimatedGapMinutes: Int,
    val recordedDays: Int,
    val averagePrimarySleepMinutes: Int,
    val consideredDays: Int = SLEEP_STATS_DAYS
)

sealed class SocialJetLagResult {
    data object Incomplete : SocialJetLagResult()
    data class Complete(
        val workdayMidpointMinutes: Int,
        val freeDayMidpointMinutes: Int,
        val differenceMinutes: Int
    ) : SocialJetLagResult() {
        val differenceHours: Double get() = differenceMinutes / 60.0
    }
}

object SleepStatsCalculator {
    // 统计窗口内、按日期去重的齐全记录（半成品记录不计入统计，见 CONTEXT.md）
    internal fun recentCompleteRecords(records: List<SleepRecord>, today: LocalDate): Collection<SleepRecord> {
        val start = today.minusDays(SLEEP_STATS_DAYS.toLong())
        return records.filter { it.date >= start && it.date < today && it.isComplete }
            .associateBy { it.date }
            .values
    }

    fun summarize(
        records: List<SleepRecord>,
        targetMinutes: Int,
        today: LocalDate = LocalDate.now()
    ): SleepGapSummary {
        require(SleepSettings(targetMinutes).targetMinutes == targetMinutes)
        val recent = recentCompleteRecords(records, today)
        val gap = recent.sumOf { (targetMinutes - (it.effectivePrimarySleepMinutes ?: 0) - it.napMinutes).coerceAtLeast(0) }
        return SleepGapSummary(
            estimatedGapMinutes = gap,
            recordedDays = recent.map { it.date }.distinct().size,
            averagePrimarySleepMinutes = if (recent.isEmpty()) 0 else recent.sumOf { it.effectivePrimarySleepMinutes ?: 0 } / recent.size
        )
    }
}

object SocialJetLagCalculator {
    fun calculate(records: List<SleepRecord>, today: LocalDate = LocalDate.now()): SocialJetLagResult {
        // 半成品记录没有中点，不参与社交时差
        val recent = SleepStatsCalculator.recentCompleteRecords(records, today)
        val workdays = recent.filter { !it.isWeekend }
        val freeDays = recent.filter { it.isWeekend }
        if (workdays.size < 2 || freeDays.size < 2) return SocialJetLagResult.Incomplete
        val workMidpoint = circularMean(workdays.map { it.midpointMinutes!! })
        val freeMidpoint = circularMean(freeDays.map { it.midpointMinutes!! })
        val signedDifference = shortestClockDifference(workMidpoint, freeMidpoint)
        return SocialJetLagResult.Complete(workMidpoint, freeMidpoint, abs(signedDifference))
    }

    fun circularMean(minutes: List<Int>): Int {
        require(minutes.isNotEmpty())
        val angle = minutes.map { it.mod(SleepRecord.MINUTES_PER_DAY) * 2.0 * Math.PI / SleepRecord.MINUTES_PER_DAY }
        val x = angle.sumOf(::cos) / angle.size
        val y = angle.sumOf(::sin) / angle.size
        return ((atan2(y, x) * SleepRecord.MINUTES_PER_DAY / (2.0 * Math.PI)).roundToInt()).mod(SleepRecord.MINUTES_PER_DAY)
    }

    private fun shortestClockDifference(first: Int, second: Int): Int =
        (second - first + SleepRecord.MINUTES_PER_DAY / 2).mod(SleepRecord.MINUTES_PER_DAY) - SleepRecord.MINUTES_PER_DAY / 2
}

data class TwoProcessPoint(
    val offsetMinutes: Int,
    val clockTime: LocalTime,
    val processS: Double,
    val circadianAlertness: Double,
    val sleepTendency: Double,
    val isAsleep: Boolean
)

object TwoProcessModel {
    const val STEP_MINUTES = 30
    const val POINTS = 24 * 60 / STEP_MINUTES
    const val TAU_AWAKE_HOURS = 18.2
    const val TAU_SLEEP_HOURS = 4.2
    const val PROCESS_S_LOWER = 0.0
    const val PROCESS_S_UPPER = 1.0
    const val DEFAULT_PHASE_MINUTES = 3 * 60

    fun generate(
        records: List<SleepRecord>,
        chronotype: ChronotypeProfile?,
        start: java.time.LocalDateTime,
        targetMinutes: Int = DEFAULT_SLEEP_TARGET_MINUTES
    ): List<TwoProcessPoint> {
        // 只用齐全记录推睡眠窗口；半成品记录缺一端无法建模
        val latest = records.filter { it.isComplete }.maxByOrNull { it.date }
        val phase = chronotype?.midpointMinutes ?: DEFAULT_PHASE_MINUTES
        val sleepStart = latest?.bedtime?.toMinuteOfDay()
            ?: (start.toLocalTime().toMinuteOfDay() - targetMinutes).mod(SleepRecord.MINUTES_PER_DAY)
        val sleepEnd = latest?.wakeTime?.toMinuteOfDay() ?: start.toLocalTime().toMinuteOfDay()
        val startsInSleep = isInSleep(start.toLocalTime().toMinuteOfDay(), sleepStart, sleepEnd)
        var processS = if (startsInSleep) 0.75 else 0.35
        return (0 until POINTS).map { index ->
            val offset = index * STEP_MINUTES
            val clock = start.plusMinutes(offset.toLong()).toLocalTime()
            val asleep = isInSleep(clock.toMinuteOfDay(), sleepStart, sleepEnd)
            if (index > 0) processS = processSAt(processS, asleep, STEP_MINUTES)
            val alertness = processC(clock.toMinuteOfDay(), phase)
            val tendency = (processS * 0.6 + (1.0 - alertness) * 0.4).coerceIn(0.0, 1.0)
            TwoProcessPoint(offset, clock, processS, alertness, tendency, asleep)
        }
    }

    fun processSAt(value: Double, asleep: Boolean, minutes: Int): Double {
        val tauHours = if (asleep) TAU_SLEEP_HOURS else TAU_AWAKE_HOURS
        val target = if (asleep) PROCESS_S_LOWER else PROCESS_S_UPPER
        val next = target + (value - target) * exp(-minutes / (tauHours * 60.0))
        return next.coerceIn(PROCESS_S_LOWER, PROCESS_S_UPPER)
    }

    fun processC(clockMinutes: Int, phaseMinutes: Int): Double {
        // Chronotype midpoint denotes the modeled alertness trough; invert cosine so it is a low point.
        val radians = 2.0 * Math.PI * (clockMinutes - phaseMinutes) / SleepRecord.MINUTES_PER_DAY
        return (0.5 - 0.5 * cos(radians)).coerceIn(0.0, 1.0)
    }

    private fun isInSleep(clockMinutes: Int, sleepStart: Int, sleepEnd: Int): Boolean =
        if (sleepStart <= sleepEnd) clockMinutes in sleepStart until sleepEnd
        else clockMinutes >= sleepStart || clockMinutes < sleepEnd
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
