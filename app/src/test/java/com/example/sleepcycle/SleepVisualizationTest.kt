package com.example.sleepcycle

import com.example.sleepcycle.data.InMemorySleepRecordRepository
import com.example.sleepcycle.model.SleepGoalLevel
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepVisualizationCalculator
import com.example.sleepcycle.ui.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * 工单 #10（可视化·数据序列基座 + 时长趋势柱状图）：
 * 逐日序列、达标四档、统计窗口切换与点选。
 * 词汇见 CONTEXT.md（达标日、统计窗口、半成品记录）。
 */
class SleepVisualizationTest {
    private val today: LocalDate = LocalDate.now()

    private fun completeRecord(date: LocalDate, primary: Int) =
        SleepRecord(date, LocalTime.of(23, 0), LocalTime.of(7, 0), primary, 0)

    @Test
    fun dailyStatsCoversWholeWindowIncludingToday() {
        val stats = SleepVisualizationCalculator.dailyStats(emptyList(), windowDays = 7, targetMinutes = 480, today = today)
        assertEquals(7, stats.size)
        assertEquals(today.minusDays(6), stats.first().date)
        assertEquals(today, stats.last().date)
        stats.forEach { assertEquals(SleepGoalLevel.NO_RECORD, it.goalLevel) }
    }

    @Test
    fun dailyStatsMapsCompleteRecordWithAttainmentLevel() {
        val record = completeRecord(today, primary = 480)
        val stats = SleepVisualizationCalculator.dailyStats(listOf(record), windowDays = 3, targetMinutes = 480, today = today)
        val todayStat = stats.last()
        assertEquals(SleepGoalLevel.ON_TARGET, todayStat.goalLevel)
        assertEquals(480, todayStat.primarySleepMinutes)
        assertEquals(LocalTime.of(23, 0), todayStat.bedtime)
        assertEquals(LocalTime.of(7, 0), todayStat.wakeTime)
    }

    @Test
    fun attainmentBoundariesSixtyMinuteGap() {
        // 缺口恰好 60 分钟 → 接近；61 分钟 → 缺口大
        val near = SleepVisualizationCalculator.dailyStats(listOf(completeRecord(today, 420)), 1, 480, today).single()
        assertEquals(SleepGoalLevel.NEAR_TARGET, near.goalLevel)
        val large = SleepVisualizationCalculator.dailyStats(listOf(completeRecord(today, 419)), 1, 480, today).single()
        assertEquals(SleepGoalLevel.LARGE_GAP, large.goalLevel)
    }

    @Test
    fun halfRecordsAndMissingDaysAreNoRecordGaps() {
        val half = SleepRecord(today, null, LocalTime.of(7, 0), null)
        val stats = SleepVisualizationCalculator.dailyStats(listOf(half), windowDays = 2, targetMinutes = 480, today = today)
        val todayStat = stats.last()
        assertEquals(SleepGoalLevel.NO_RECORD, todayStat.goalLevel)
        assertNull(todayStat.primarySleepMinutes)
        assertNull(todayStat.bedtime)
    }

    @Test
    fun windowSwitchClearsSelectionAndKeepsSummaryWindowUntouched() = runBlocking {
        val repository = InMemorySleepRecordRepository(listOf(completeRecord(today.minusDays(1), 480)))
        val viewModel = SleepViewModel(
            sleepRecordRepository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
        viewModel.toggleStatDate(today)
        viewModel.setVisualizationWindow(30)

        assertEquals(30, viewModel.uiState.value.visualizationWindowDays)
        assertNull("切换窗口应清空点选", viewModel.uiState.value.selectedStatDate)
        assertEquals(30, viewModel.uiState.value.dailySleepStats.size)
        assertEquals("概要卡统计口径固定 14 天", 14, viewModel.uiState.value.sleepGapSummary.consideredDays)
    }

    @Test
    fun toggleStatDateSelectsThenDeselects() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = SleepViewModel(
            sleepRecordRepository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
        viewModel.toggleStatDate(today)
        assertEquals(today, viewModel.uiState.value.selectedStatDate)
        viewModel.toggleStatDate(today)
        assertNull(viewModel.uiState.value.selectedStatDate)
        viewModel.toggleStatDate(today.minusDays(1))
        viewModel.toggleStatDate(today)
        assertEquals("点选另一天应直接切换", today, viewModel.uiState.value.selectedStatDate)
    }

    @Test
    fun dailyStatsFollowRecordsAndWindowInViewModel() = runBlocking {
        val repository = InMemorySleepRecordRepository(listOf(completeRecord(today.minusDays(2), 300)))
        val viewModel = SleepViewModel(
            sleepRecordRepository = repository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
        viewModel.setVisualizationWindow(7)

        val stats = viewModel.uiState.value.dailySleepStats
        assertEquals(7, stats.size)
        assertEquals(300, stats.first { it.date == today.minusDays(2) }.primarySleepMinutes)
        assertEquals(SleepGoalLevel.LARGE_GAP, stats.first { it.date == today.minusDays(2) }.goalLevel)
    }
}
