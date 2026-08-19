package com.example.sleepcycle.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 睡眠质量与周期评估评级
 */
enum class SleepQuality(val label: String, val description: String) {
    EXCELLENT("极佳", "精力充沛，神清气爽（推荐）"),
    OPTIMAL("充足", "符合成年人充足睡眠标准"),
    SUFFICIENT("尚可", "可维持日常基本专注力"),
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
}
