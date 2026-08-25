package com.example.sleepcycle

import com.example.sleepcycle.data.InMemoryChronotypeProfileRepository
import com.example.sleepcycle.model.ChronotypeAnswers
import com.example.sleepcycle.model.ChronotypeCalculator
import com.example.sleepcycle.ui.ChronotypeSaveState
import com.example.sleepcycle.ui.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ChronotypeViewModelTest {
    private val answers = ChronotypeAnswers(
        workdayBedtime = LocalTime.of(22, 30),
        workdaySleepTime = LocalTime.of(23, 30),
        workdayWakeTime = LocalTime.of(7, 30),
        freeDayBedtime = LocalTime.of(0, 30),
        freeDaySleepTime = LocalTime.of(1, 0),
        freeDayWakeTime = LocalTime.of(9, 0),
        needsAlarm = true
    )

    @Test
    fun saveThenRecreateViewModelRestoresChronotypeProfile() {
        val repository = InMemoryChronotypeProfileRepository()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val first = SleepViewModel(chronotypeRepository = repository, externalScope = scope)
        first.beginChronotypeEdit()
        first.updateChronotypeAnswers(answers)
        first.saveChronotype()

        assertEquals(ChronotypeSaveState.Saved, first.uiState.value.chronotypeSaveState)
        assertEquals(ChronotypeCalculator.calculate(answers).category, first.uiState.value.chronotypeProfile?.category)

        val recreated = SleepViewModel(chronotypeRepository = repository, externalScope = scope)
        assertEquals(answers, recreated.uiState.value.chronotypeAnswers)
        assertEquals(first.uiState.value.chronotypeProfile, recreated.uiState.value.chronotypeProfile)
    }

    @Test
    fun cancellingEditKeepsSavedProfileAndAnswers() {
        val repository = InMemoryChronotypeProfileRepository(ChronotypeCalculator.calculate(answers, 1L))
        val viewModel = SleepViewModel(chronotypeRepository = repository, externalScope = CoroutineScope(Dispatchers.Unconfined))
        val original = viewModel.uiState.value.chronotypeAnswers
        viewModel.beginChronotypeEdit()
        viewModel.updateChronotypeAnswers(original.copy(workdayWakeTime = LocalTime.of(10, 0)))
        viewModel.cancelChronotypeEdit()

        assertEquals(original, viewModel.uiState.value.chronotypeAnswers)
        assertEquals(LocalTime.of(7, 30), viewModel.uiState.value.chronotypeProfile?.answers?.workdayWakeTime)
    }

    @Test
    fun saveFailureIsExposedAndUncommittedProfileRemains() {
        val repository = InMemoryChronotypeProfileRepository(failOnSave = true)
        val viewModel = SleepViewModel(chronotypeRepository = repository, externalScope = CoroutineScope(Dispatchers.Unconfined))
        viewModel.beginChronotypeEdit()
        viewModel.updateChronotypeAnswers(answers)
        viewModel.saveChronotype()

        assertTrue(viewModel.uiState.value.chronotypeSaveState is ChronotypeSaveState.Error)
        assertEquals(null, viewModel.uiState.value.chronotypeProfile)
        assertTrue(viewModel.uiState.value.morningLightGuidance != null)
        assertTrue(viewModel.uiState.value.digitalSunsetGuidance != null)
    }
}
