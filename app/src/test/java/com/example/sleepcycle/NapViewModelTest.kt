package com.example.sleepcycle

import com.example.sleepcycle.model.NapType
import com.example.sleepcycle.ui.NapEvent
import com.example.sleepcycle.ui.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class NapViewModelTest {

    @Test
    fun selectingShortNapCreatesAlarmRequestWithSupportedDuration() {
        val viewModel = SleepViewModel()

        viewModel.selectNapType(NapType.TEN_MINUTES)

        val request = viewModel.uiState.value.napAlarmRequest
        assertEquals(NapType.TEN_MINUTES, request?.napType)
        assertTrue(request != null)
        assertTrue(request!!.targetTime != LocalTime.now())
    }

    @Test
    fun coffeeNapRequiresConfirmationBeforeAlarmRequest() = runBlocking {
        val viewModel = SleepViewModel(externalScope = CoroutineScope(Dispatchers.Unconfined))
        var event: NapEvent? = null
        val job = launch(Dispatchers.Unconfined) { event = viewModel.napEvents.first() }

        viewModel.selectNapType(NapType.COFFEE_NAP)

        assertTrue(viewModel.uiState.value.showCoffeeNapPrompt)
        assertEquals(null, viewModel.uiState.value.napAlarmRequest)
        assertTrue(event is NapEvent.CoffeeNapPrompt)

        viewModel.confirmCoffeeNap()
        assertEquals(NapType.COFFEE_NAP, viewModel.uiState.value.napAlarmRequest?.napType)
        assertEquals(20, viewModel.uiState.value.napAlarmRequest?.napType?.durationMinutes)
        job.cancel()
    }

    @Test
    fun markingNapAlarmSetShowsSleepInertiaGuidanceAndClearsRequest() {
        val viewModel = SleepViewModel()

        viewModel.selectNapType(NapType.TWENTY_MINUTES)
        viewModel.markNapAlarmSet()

        assertEquals(null, viewModel.uiState.value.napAlarmRequest)
        assertTrue(viewModel.uiState.value.wakeUpGuidance!!.contains("15–60 分钟"))
        assertTrue(viewModel.uiState.value.wakeUpGuidance!!.contains("晨光"))
    }
}
