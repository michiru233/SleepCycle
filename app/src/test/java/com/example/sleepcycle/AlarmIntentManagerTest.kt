package com.example.sleepcycle

import com.example.sleepcycle.alarm.AlarmConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalTime

class AlarmConfigTest {

    @Test
    fun testAlarmConfigCreation() {
        val targetTime = LocalTime.of(7, 30)
        val config = AlarmConfig.fromLocalTime(targetTime, "晨间唤醒")

        assertEquals(7, config.hour)
        assertEquals(30, config.minute)
        assertEquals("晨间唤醒", config.message)
        assertFalse(config.skipUi)
    }

    @Test
    fun testAlarmConfigMidnight() {
        val targetTime = LocalTime.of(0, 0)
        val config = AlarmConfig.fromLocalTime(targetTime)

        assertEquals(0, config.hour)
        assertEquals(0, config.minute)
        assertEquals("睡眠周期智能唤醒", config.message)
        assertFalse(config.skipUi)
    }
}
