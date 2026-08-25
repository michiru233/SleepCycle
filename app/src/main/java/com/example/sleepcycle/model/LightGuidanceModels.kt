package com.example.sleepcycle.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

sealed class LightGuidance {
    abstract val targetTime: LocalTime
    abstract val windowStart: LocalTime
    abstract val windowEnd: LocalTime
    abstract val durationMinutes: Int
    abstract val note: String

    data class MorningLight(
        override val targetTime: LocalTime,
        override val windowStart: LocalTime,
        override val windowEnd: LocalTime,
        override val durationMinutes: Int = 20,
        override val note: String = "起床后尽快接触户外光 10–30 分钟；光照反应存在个体差异。"
    ) : LightGuidance()

    data class DigitalSunset(
        override val targetTime: LocalTime,
        override val windowStart: LocalTime,
        override val windowEnd: LocalTime,
        override val durationMinutes: Int = 60,
        override val note: String = "建议在目标入睡时间前 60–120 分钟开始数字日落，减少夜间屏幕刺激；蓝光过滤器不被本建议视为确定有效的治疗，个体差异请自行观察。"
    ) : LightGuidance()
}

object LightGuidanceCalculator {
    private const val MORNING_MINUTES = 20
    private const val DIGITAL_SUNSET_MINUTES = 60
    private const val WINDOW_MINUTES = 15
    private val FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    fun morningLight(wakeTime: LocalTime, chronotype: ChronotypeProfile? = null): LightGuidance.MorningLight {
        val target = wakeTime.plusMinutes(5)
        return LightGuidance.MorningLight(
            targetTime = target,
            windowStart = target.plusMinutes(-WINDOW_MINUTES.toLong()),
            windowEnd = target.plusMinutes(WINDOW_MINUTES.toLong())
        )
    }

    fun digitalSunset(bedtime: LocalTime, chronotype: ChronotypeProfile? = null): LightGuidance.DigitalSunset {
        val target = bedtime.plusMinutes(-DIGITAL_SUNSET_MINUTES.toLong())
        return LightGuidance.DigitalSunset(
            targetTime = target,
            windowStart = bedtime.plusMinutes(-120),
            windowEnd = bedtime.plusMinutes(-60)
        )
    }

    fun format(guidance: LightGuidance): String = "${guidance.windowStart.format(FORMATTER)}–${guidance.windowEnd.format(FORMATTER)}"
}
