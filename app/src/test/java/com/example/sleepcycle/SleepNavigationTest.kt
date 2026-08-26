package com.example.sleepcycle

import com.example.sleepcycle.ui.SleepDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepNavigationTest {
    @Test
    fun homeIsFocusedCalculationDestination() {
        assertEquals("睡眠计算", SleepDestination.HOME.label)
    }

    @Test
    fun drawerExposesExactlyTheSixSecondaryDestinations() {
        val secondary = SleepDestination.entries.filter { it != SleepDestination.HOME }
        assertEquals(6, secondary.size)
        assertEquals(
            listOf("睡眠记录", "睡眠分析", "时间型与光照", "小睡工具", "睡眠知识", "设置/关于"),
            secondary.map { it.label }
        )
        assertTrue(secondary.all { it.label.isNotBlank() })
    }
}
