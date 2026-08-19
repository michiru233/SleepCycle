package com.example.sleepcycle.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import java.time.LocalTime

/**
 * 闹钟参数结构
 */
data class AlarmConfig(
    val hour: Int,
    val minute: Int,
    val message: String,
    val skipUi: Boolean = false
) {
    companion object {
        fun fromLocalTime(
            targetTime: LocalTime,
            message: String = "睡眠周期智能唤醒",
            skipUi: Boolean = false
        ): AlarmConfig {
            return AlarmConfig(
                hour = targetTime.hour,
                minute = targetTime.minute,
                message = message,
                skipUi = skipUi
            )
        }
    }
}

object AlarmIntentManager {

    /**
     * 创建用于设置原生系统闹钟的 Intent
     */
    fun createSetAlarmIntent(config: AlarmConfig): Intent {
        return Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, config.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, config.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, config.message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, config.skipUi)
        }
    }

    /**
     * 根据时间直接创建 Intent
     */
    fun createSetAlarmIntent(
        targetTime: LocalTime,
        message: String = "睡眠周期智能唤醒"
    ): Intent {
        return createSetAlarmIntent(AlarmConfig.fromLocalTime(targetTime, message))
    }

    /**
     * 调用系统闹钟应用设置闹钟
     */
    fun setAlarm(
        context: Context,
        targetTime: LocalTime,
        message: String = "睡眠周期智能唤醒"
    ): Boolean {
        val intent = createSetAlarmIntent(targetTime, message).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            Toast.makeText(
                context,
                "正在为您打开闹钟: ${String.format("%02d:%02d", targetTime.hour, targetTime.minute)}",
                Toast.LENGTH_SHORT
            ).show()
            true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "未找到系统闹钟应用，请手动设置: ${String.format("%02d:%02d", targetTime.hour, targetTime.minute)}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
