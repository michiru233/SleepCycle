package com.example.sleepcycle

import com.example.sleepcycle.model.SLEEP_INERTIA_GUIDANCE
import com.example.sleepcycle.ui.SleepViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepInertiaGuidanceTest {

    @Test
    fun settingAnySleepAlarmExposesTheBufferGuidance() {
        val viewModel = SleepViewModel()

        viewModel.showSleepInertiaGuidance()

        assertEquals(SLEEP_INERTIA_GUIDANCE, viewModel.uiState.value.wakeUpGuidance)
        assertTrue(viewModel.uiState.value.wakeUpGuidance!!.contains("轻度活动"))
    }
}
