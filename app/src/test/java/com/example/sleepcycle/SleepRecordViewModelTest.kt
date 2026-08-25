package com.example.sleepcycle

import com.example.sleepcycle.data.InMemorySleepRecordRepository
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepSettings
import com.example.sleepcycle.ui.SleepRecordSaveState
import com.example.sleepcycle.ui.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SleepRecordViewModelTest {
    private val yesterday = LocalDate.now().minusDays(1)

    private fun viewModel(repository: InMemorySleepRecordRepository): SleepViewModel = SleepViewModel(
        sleepRecordRepository = repository,
        externalScope = CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun initialLoadExposesRecordsAndTarget() {
        val record = SleepRecord(yesterday, LocalTime.of(23, 0), LocalTime.of(7, 0), 480, 20)
        val viewModel = viewModel(InMemorySleepRecordRepository(listOf(record), SleepSettings(450)))
        assertEquals(listOf(record), viewModel.uiState.value.sleepRecords)
        assertEquals(450, viewModel.uiState.value.sleepSettings.targetMinutes)
    }

    @Test
    fun saveUpsertsSameDateAndReloadsState() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        viewModel.updateSleepRecordForm(date = yesterday, bedtime = LocalTime.of(23, 30), wakeTime = LocalTime.of(7, 0), primarySleepMinutes = 450, napMinutes = 15)
        viewModel.saveSleepRecord()
        assertEquals(SleepRecordSaveState.Saved, viewModel.uiState.value.recordSaveState)
        assertEquals(1, viewModel.uiState.value.sleepRecords.size)
        assertEquals(450, viewModel.uiState.value.sleepRecords.single().primarySleepMinutes)
        viewModel.updateSleepRecordForm(wakeTime = LocalTime.of(8, 0), primarySleepMinutes = 510)
        viewModel.saveSleepRecord()
        assertEquals(1, repository.loadRecords().size)
        assertEquals(510, viewModel.uiState.value.sleepRecords.single().primarySleepMinutes)
    }

    @Test
    fun cancelEditRestoresOriginalRecordDraftAndLeavesRepositoryUntouched() = runBlocking {
        val original = SleepRecord(yesterday, LocalTime.of(23, 0), LocalTime.of(7, 0), 480, 20)
        val repository = InMemorySleepRecordRepository(listOf(original))
        val viewModel = viewModel(repository)
        viewModel.editSleepRecord(yesterday)
        viewModel.updateSleepRecordForm(bedtime = LocalTime.of(22, 0), primarySleepMinutes = 420, napMinutes = 0)
        viewModel.cancelSleepRecordEdit()
        val state = viewModel.uiState.value
        assertEquals(original.date, state.recordDate)
        assertEquals(original.bedtime, state.recordBedtime)
        assertEquals(original.wakeTime, state.recordWakeTime)
        assertEquals(original.primarySleepMinutes, state.recordPrimarySleepMinutes)
        assertEquals(original.napMinutes, state.recordNapMinutes)
        assertEquals(null, state.editingSleepRecord)
        assertEquals(listOf(original), repository.loadRecords())
    }

    @Test
    fun deleteRemovesRecordAndRefreshesStats() {
        val record = SleepRecord(yesterday, LocalTime.of(23, 0), LocalTime.of(7, 0), 480)
        val repository = InMemorySleepRecordRepository(listOf(record))
        val viewModel = viewModel(repository)
        viewModel.deleteSleepRecord(yesterday)
        assertTrue(viewModel.uiState.value.sleepRecords.isEmpty())
        assertEquals(0, viewModel.uiState.value.sleepGapSummary.recordedDays)
    }

    @Test
    fun targetSaveUsesFifteenMinuteSettingsAndUpdatesGap() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        viewModel.saveSleepTarget(525)
        assertEquals(525, viewModel.uiState.value.sleepSettings.targetMinutes)
        assertEquals(525, repository.loadSettings().targetMinutes)
        viewModel.saveSleepTarget(530)
        assertTrue(viewModel.uiState.value.sleepDataError?.contains("15") == true)
    }

    @Test
    fun repositoryFailureIsVisible() {
        val viewModel = viewModel(InMemorySleepRecordRepository(failOnWrite = true))
        viewModel.updateSleepRecordForm(date = yesterday)
        viewModel.saveSleepRecord()
        assertTrue(viewModel.uiState.value.recordSaveState is SleepRecordSaveState.Error)
    }
}
