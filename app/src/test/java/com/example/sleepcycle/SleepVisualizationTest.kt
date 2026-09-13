package com.example.sleepcycle

import com.example.sleepcycle.data.InMemorySleepRecordRepository
import com.example.sleepcycle.model.SleepGoalLevel
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepVisualizationCalculator
import com.example.sleepcycle.model.bandAxis
import com.example.sleepcycle.model.bandPositions
import com.example.sleepcycle.ui.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // —— 工单 #11：作息带状图跨午夜对齐 ——

    private fun bandStat(date: LocalDate, bed: LocalTime, wake: LocalTime) =
        SleepRecord(date, bed, wake, SleepRecord.durationBetween(bed, wake), 0)

    @Test
    fun bandAxisReturnsNullWithoutCompleteRecords() {
        val half = SleepRecord(today, null, LocalTime.of(7, 0), null)
        val stats = SleepVisualizationCalculator.dailyStats(listOf(half), windowDays = 1, targetMinutes = 480, today = today)
        assertNull(bandAxis(stats))
        assertNull(bandAxis(SleepVisualizationCalculator.dailyStats(emptyList(), 1, 480, today)))
    }

    @Test
    fun bandAxisCoversTypicalEveningDataWithoutBreak() {
        val records = (0 until 3).map { bandStat(today.minusDays(it.toLong()), LocalTime.of(23, 0), LocalTime.of(7, 0)) }
        val stats = SleepVisualizationCalculator.dailyStats(records, windowDays = 3, targetMinutes = 480, today = today)
        val axis = bandAxis(stats)!!
        val positions = bandPositions(axis, stats).filterNotNull()
        assertEquals(3, positions.size)
        positions.forEach { (start, end) ->
            assertTrue("横带必须在轴范围内且不断裂", start >= 0f && end <= 1f && end > start)
        }
    }

    @Test
    fun bandAxisFallsBackToNoonAnchorWhenEarlyMorningBedtimeBreaksAxis() {
        // 凌晨 00:30 入睡 + 前晚 23:00 入睡混在同一窗口：朴素轴会让 23:00 的横带越过轴末端
        val records = listOf(
            bandStat(today, LocalTime.of(0, 30), LocalTime.of(8, 0)),
            bandStat(today.minusDays(1), LocalTime.of(23, 0), LocalTime.of(7, 0))
        )
        val stats = SleepVisualizationCalculator.dailyStats(records, windowDays = 2, targetMinutes = 480, today = today)
        val axis = bandAxis(stats)!!
        val positions = bandPositions(axis, stats).filterNotNull()
        assertEquals(2, positions.size)
        positions.forEach { (start, end) ->
            assertTrue("回退到正午锚点后所有横带不断裂", start >= 0f && end <= 1f && end > start)
        }
    }

    @Test
    fun bandPositionsSkipIncompleteDays() {
        val half = SleepRecord(today.minusDays(1), null, LocalTime.of(7, 0), null)
        val records = listOf(bandStat(today, LocalTime.of(23, 0), LocalTime.of(7, 0)), half)
        val stats = SleepVisualizationCalculator.dailyStats(records, windowDays = 2, targetMinutes = 480, today = today)
        val axis = bandAxis(stats)!!
        val positions = bandPositions(axis, stats)
        assertNull("半成品日应留空档", positions[0])
        val band = positions[1]!!
        assertTrue("完整记录应有横带", band.first >= 0f && band.second <= 1f && band.second > band.first)
    }
}
