package com.example.sleepcycle.data

import android.content.Context
import android.content.SharedPreferences
import com.example.sleepcycle.model.SleepCalculator

/**
 * 睡眠偏好设置存储仓库接口
 */
interface SleepPreferencesRepository {
    fun getLatencyMinutes(): Int
    fun setLatencyMinutes(latencyMinutes: Int)
}

/**
 * 内存中实现的偏好存储（用于默认回退及纯单元测试）
 */
class InMemorySleepPreferencesRepository(
    initialLatency: Int = SleepCalculator.DEFAULT_FALL_ASLEEP_LATENCY_MINUTES
) : SleepPreferencesRepository {
    private var latency: Int = initialLatency.coerceIn(0, 60)

    override fun getLatencyMinutes(): Int = latency

    override fun setLatencyMinutes(latencyMinutes: Int) {
        this.latency = latencyMinutes.coerceIn(0, 60)
    }
}

/**
 * 基于 SharedPreferences 实现的用户偏好持久化管理
 */
class SharedPreferencesSleepPreferencesRepository(
    private val sharedPreferences: SharedPreferences
) : SleepPreferencesRepository {

    companion object {
        const val PREFS_NAME = "sleep_cycle_prefs"
        const val KEY_LATENCY_MINUTES = "key_latency_minutes"

        fun create(context: Context): SharedPreferencesSleepPreferencesRepository {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SharedPreferencesSleepPreferencesRepository(prefs)
        }
    }

    override fun getLatencyMinutes(): Int {
        return sharedPreferences.getInt(
            KEY_LATENCY_MINUTES,
            SleepCalculator.DEFAULT_FALL_ASLEEP_LATENCY_MINUTES
        )
    }

    override fun setLatencyMinutes(latencyMinutes: Int) {
        sharedPreferences.edit()
            .putInt(KEY_LATENCY_MINUTES, latencyMinutes.coerceIn(0, 60))
            .apply()
    }
}
