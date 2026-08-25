package com.example.sleepcycle.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Short-rest presets backed by the in-app nap flow.
 */
enum class NapType(
    val durationMinutes: Int,
    val label: String,
    val description: String,
    val wakeUpTip: String,
    val isCoffeeNap: Boolean = false
) {
    TEN_MINUTES(
        durationMinutes = 10,
        label = "10 分钟",
        description = "快速提神，尽量减少醒后昏沉",
        wakeUpTip = "醒来后接触晨光并做几分钟轻度活动，帮助恢复清醒。"
    ),
    TWENTY_MINUTES(
        durationMinutes = 20,
        label = "20 分钟",
        description = "短时恢复专注力",
        wakeUpTip = "醒来后先接触明亮光线，再做轻度活动，给大脑几分钟恢复时间。"
    ),
    COFFEE_NAP(
        durationMinutes = 20,
        label = "Coffee nap",
        description = "先喝咖啡，再小睡 20 分钟",
        wakeUpTip = "咖啡因通常需要一段时间起效；醒来后接触晨光并做轻度活动。",
        isCoffeeNap = true
    ),
    ONE_CYCLE_90_MINUTES(
        durationMinutes = 90,
        label = "90 分钟（1 周期）",
        description = "完整经历一个睡眠周期",
        wakeUpTip = "醒来后预留缓冲时间，接触晨光并做轻度活动。"
    )
}

data class NapAlarmRequest(
    val napType: NapType,
    val targetTime: LocalTime
)

const val SLEEP_INERTIA_GUIDANCE =
    "醒后约 15–60 分钟认知可能未完全恢复。建议接触晨光并做轻度活动；个体差异，仅供参考。"

/**
 * 睡眠质量与周期评估评级
 */
enum class SleepQuality(val label: String, val description: String) {
    EXCELLENT("极佳", "精力充沛，神清气爽（推荐）"),
    OPTIMAL("充足", "符合成年人充足睡眠标准"),
    SUFFICIENT("尚可", "可维持日常基本专注力"),
    RECHARGE("补能", "适合午后深度补觉或应急休整"),
    NAP("午睡", "1个完整周期，快速恢复精力且不昏沉"),
    SHORT("勉强", "稍短，可能偶感疲惫")
}

/**
 * 睡眠周期计算结果项
 *
 * @param cycleCount 睡眠周期数量 (通常 3~6 个)
 * @param targetTime 目标时间 (正推为起床时间，倒推为入睡时间)
 * @param totalMinutes 睡眠总时长（分钟，不含入睡潜伏期）
 * @param quality 睡眠质量评级
 * @param isRecommended 是否为推荐高亮项 (5周期 / 7.5小时)
 */
data class SleepRecommendation(
    val cycleCount: Int,
    val targetTime: LocalTime,
    val totalMinutes: Int,
    val quality: SleepQuality,
    val isRecommended: Boolean
) {
    val totalHoursText: String
        get() {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (mins == 0) "${hours}小时" else "${hours}小时${mins}分"
        }

    val formattedTargetTime: String
        get() = targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    /**
     * 最佳唤醒窗口 (目标时间前 15 分钟 到 后 15 分钟, 宽 30 分钟)
     *
     * 用于弱化单点闹钟的不确定性：真实睡眠周期在 80~100 分钟间波动、入睡潜伏期因人而异，
     * 单个精确钟点并不科学，窗口仅作提示与行为参考。跨天自动翻转 (LocalTime 自动取模)。
     */
    val wakeWindowText: String
        get() {
            val start = targetTime.minusMinutes(15)
            val end = targetTime.plusMinutes(15)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return "${start.format(formatter)}–${end.format(formatter)}"
        }
}
