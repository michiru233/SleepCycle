package com.example.sleepcycle

import com.example.sleepcycle.data.InMemorySleepRecordRepository
import com.example.sleepcycle.model.NapType
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.ui.SleepViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * 工单 #7（实测反馈：设闹钟一次写入两端）：
 * 推荐时间卡设闹钟时，入睡 = 按下按钮时刻 + 入睡潜伏期，起床 = 闹钟响铃时间；
 * 「+15 分钟」独立按钮移除。睡眠日归属、睡眠中占位、后来者覆盖、Snackbar 反馈保持。
 * 词汇见 CONTEXT.md（睡眠计划联动、睡眠日）；决策依据 ADR-0001、ADR-0002。
 */
class SleepLinkageViewModelTest {
    private fun viewModel(repository: InMemorySleepRecordRepository): SleepViewModel = SleepViewModel(
        sleepRecordRepository = repository,
        externalScope = CoroutineScope(Dispatchers.Unconfined)
    )

    private fun snackbarEvents(viewModel: SleepViewModel, block: () -> Unit): List<String> {
        val events = mutableListOf<String>()
        val collector = CoroutineScope(Dispatchers.Unconfined).launch { viewModel.quickRecordEvents.collect { events.add(it) } }
        try {
            block()
        } finally {
            collector.cancel()
        }
        return events
    }

    @Test
    fun alarmPlanWritesBothEndsForEveningSet() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        val today = LocalDate.now()

        val events = snackbarEvents(viewModel) {
            viewModel.recordSleepPlan(targetTime = LocalTime.of(7, 0), latencyMinutes = 20, now = LocalTime.of(23, 0), today = today)
        }

        val record = repository.loadRecords().single()
        assertEquals("晚间设定归属次日睡眠日", today.plusDays(1), record.date)
        assertEquals("入睡 = 按下按钮时刻 + 潜伏期", LocalTime.of(23, 20), record.bedtime)
        assertEquals("起床 = 闹钟响铃时间", LocalTime.of(7, 0), record.wakeTime)
        assertEquals("23:20 到 07:00 自动结算 460 分钟", 460, record.primarySleepMinutes)
        assertTrue(events.single().contains("23:20"))
    }

    @Test
    fun alarmPlanAttributesToCurrentDayWhenSetAfterMidnight() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        val today = LocalDate.now()

        viewModel.recordSleepPlan(targetTime = LocalTime.of(7, 0), latencyMinutes = 20, now = LocalTime.of(0, 30), today = today)

        val record = repository.loadRecords().single()
        assertEquals("凌晨（已在睡眠中）设定归属当天", today, record.date)
        assertEquals(LocalTime.of(0, 50), record.bedtime)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
        assertEquals("00:50 到 07:00 结算 370 分钟", 370, record.primarySleepMinutes)
    }

    @Test
    fun alarmPlanOverwritesEarlierPlanByLaterWins() = runBlocking {
        val sleepDay = LocalDate.now().plusDays(1)
        val earlierPlan = SleepRecord(sleepDay, LocalTime.of(23, 20), LocalTime.of(7, 0), 460, 10)
        val repository = InMemorySleepRecordRepository(listOf(earlierPlan))
        val viewModel = viewModel(repository)

        snackbarEvents(viewModel) {
            viewModel.recordSleepPlan(targetTime = LocalTime.of(6, 30), latencyMinutes = 20, now = LocalTime.of(22, 40), today = sleepDay.minusDays(1))
        }

        val record = repository.loadRecords().single()
        assertEquals("后来者覆盖：入睡端更新", LocalTime.of(23, 0), record.bedtime)
        assertEquals("起床端更新为新闹钟", LocalTime.of(6, 30), record.wakeTime)
        assertEquals("重算时长 450 分钟", 450, record.primarySleepMinutes)
        assertEquals(10, record.napMinutes)
    }

    @Test
    fun alarmPlanKeepsSleepingPlaceholderState() = runBlocking {
        val sleepDay = LocalDate.now().plusDays(1)
        val sleeping = SleepRecord(sleepDay, LocalTime.of(23, 0), LocalTime.of(6, 30), 0, 0)
        val repository = InMemorySleepRecordRepository(listOf(sleeping))
        val viewModel = viewModel(repository)

        viewModel.recordSleepPlan(targetTime = LocalTime.of(7, 0), latencyMinutes = 20, now = LocalTime.of(22, 0), today = sleepDay.minusDays(1))

        val record = repository.loadRecords().single()
        assertEquals("睡眠中状态由醒来确认结束，设闹钟不改时长占位", 0, record.primarySleepMinutes)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
    }

    @Test
    fun alarmPlanReportsFailureViaSnackbar() = runBlocking {
        val repository = InMemorySleepRecordRepository(failOnWrite = true)
        val viewModel = viewModel(repository)

        val events = snackbarEvents(viewModel) { viewModel.recordSleepPlan(LocalTime.of(7, 0), latencyMinutes = 20) }

        assertTrue(events.single().contains("失败"))
    }

    @Test
    fun napAnchorWritesPresetDurationToTodayRecord() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        val today = LocalDate.now()

        val events = snackbarEvents(viewModel) {
            viewModel.selectNapType(NapType.TWENTY_MINUTES)
            viewModel.markNapAlarmSet()
        }

        val record = repository.loadRecords().single()
        assertEquals("午睡写入当天记录", today, record.date)
        assertEquals(20, record.napMinutes)
        assertNull("午睡不触碰主睡眠两端", record.bedtime)
        assertTrue(events.single().contains("20"))
    }

    @Test
    fun napAnchorOverwritesPreviousNapByLaterWins() = runBlocking {
        val existing = SleepRecord(LocalDate.now(), null, null, null, napMinutes = 20)
        val repository = InMemorySleepRecordRepository(listOf(existing))
        val viewModel = viewModel(repository)

        viewModel.selectNapType(NapType.ONE_CYCLE_90_MINUTES)
        viewModel.markNapAlarmSet()

        assertEquals("后来者覆盖：最新预设生效", 90, repository.loadRecords().single().napMinutes)
    }

    @Test
    fun napAnchorPreservesMainSleepEnds() = runBlocking {
        val today = LocalDate.now()
        val complete = SleepRecord(today, LocalTime.of(23, 0), LocalTime.of(7, 0), 480, 0)
        val repository = InMemorySleepRecordRepository(listOf(complete))
        val viewModel = viewModel(repository)

        viewModel.selectNapType(NapType.TWENTY_MINUTES)
        viewModel.markNapAlarmSet()

        val record = repository.loadRecords().single()
        assertEquals(LocalTime.of(23, 0), record.bedtime)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
        assertEquals(480, record.primarySleepMinutes)
        assertEquals(20, record.napMinutes)
    }

    @Test
    fun napAnchorReportsFailureViaSnackbar() = runBlocking {
        val repository = InMemorySleepRecordRepository(failOnWrite = true)
        val viewModel = viewModel(repository)

        val events = snackbarEvents(viewModel) {
            viewModel.selectNapType(NapType.TWENTY_MINUTES)
            viewModel.markNapAlarmSet()
        }

        assertTrue(events.single().contains("失败"))
    }
}
