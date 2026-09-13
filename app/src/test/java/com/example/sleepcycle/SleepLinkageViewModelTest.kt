package com.example.sleepcycle

import com.example.sleepcycle.data.InMemorySleepRecordRepository
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
 * 工单 #3（睡眠时间管理联动·"+15 分钟"快捷入睡）：
 * 首页时间选择卡点击后，当前时刻推后 15 分钟写入当天记录（后来者覆盖），Snackbar 反馈。
 * 词汇见 CONTEXT.md（+15 分钟、后来者覆盖、半成品记录）；决策依据 ADR-0001、ADR-0002。
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
    fun plus15WritesHalfRecordToNextSleepDayForEveningClick() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        val today = LocalDate.now()

        val events = snackbarEvents(viewModel) { viewModel.quickRecordBedtimePlus15(now = LocalTime.of(22, 45), today = today) }

        val record = repository.loadRecords().single()
        assertEquals("晚间设定归属次日睡眠日", today.plusDays(1), record.date)
        assertEquals(LocalTime.of(23, 0), record.bedtime)
        assertNull("无既有醒来数据时应保持半成品记录", record.wakeTime)
        assertNull(record.primarySleepMinutes)
        assertTrue(events.single().contains("23:00"))
    }

    @Test
    fun plus15WritesToCurrentSleepDayForAfterMidnightClick() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        val today = LocalDate.now()

        viewModel.quickRecordBedtimePlus15(now = LocalTime.of(23, 50), today = today)

        val record = repository.loadRecords().single()
        assertEquals("23:50 点击 → 入睡 00:05 仍在同一晚，归属次日", today.plusDays(1), record.date)
        assertEquals(LocalTime.of(0, 5), record.bedtime)

        val repository2 = InMemorySleepRecordRepository()
        val viewModel2 = viewModel(repository2)
        viewModel2.quickRecordBedtimePlus15(now = LocalTime.of(0, 30), today = today)
        val afterMidnightRecord = repository2.loadRecords().single()
        assertEquals("凌晨设定归属当天", today, afterMidnightRecord.date)
        assertEquals(LocalTime.of(0, 45), afterMidnightRecord.bedtime)
    }

    @Test
    fun plus15OverwritesBedtimeAndRecomputesDurationOnCompleteRecord() = runBlocking {
        val sleepDay = LocalDate.now().plusDays(1)
        val existing = SleepRecord(sleepDay, LocalTime.of(22, 0), LocalTime.of(7, 0), 540, 10)
        val repository = InMemorySleepRecordRepository(listOf(existing))
        val viewModel = viewModel(repository)

        val events = snackbarEvents(viewModel) { viewModel.quickRecordBedtimePlus15(now = LocalTime.of(22, 30), today = sleepDay.minusDays(1)) }

        val record = repository.loadRecords().single()
        assertEquals(sleepDay, record.date)
        assertEquals(LocalTime.of(22, 45), record.bedtime)
        assertEquals("后来者覆盖只改入睡端，醒来端保留", LocalTime.of(7, 0), record.wakeTime)
        assertEquals("22:45 到 07:00 重算为 495 分钟", 495, record.primarySleepMinutes)
        assertEquals(10, record.napMinutes)
        assertTrue(events.single().contains("22:45"))
    }

    @Test
    fun plus15KeepsSleepingPlaceholderState() = runBlocking {
        val sleepDay = LocalDate.now().plusDays(1)
        val sleeping = SleepRecord(sleepDay, LocalTime.of(23, 0), LocalTime.of(7, 0), 0, 0)
        val repository = InMemorySleepRecordRepository(listOf(sleeping))
        val viewModel = viewModel(repository)

        viewModel.quickRecordBedtimePlus15(now = LocalTime.of(23, 10), today = sleepDay.minusDays(1))

        val record = repository.loadRecords().single()
        assertEquals(sleepDay, record.date)
        assertEquals(LocalTime.of(23, 25), record.bedtime)
        assertEquals("睡眠中状态由醒来确认结束，+15 分钟不得改变", 0, record.primarySleepMinutes)
    }

    @Test
    fun plus15CompletesWakeOnlyHalfRecord() = runBlocking {
        val sleepDay = LocalDate.now().plusDays(1)
        val wakeOnly = SleepRecord(sleepDay, null, LocalTime.of(7, 0), null, 0)
        val repository = InMemorySleepRecordRepository(listOf(wakeOnly))
        val viewModel = viewModel(repository)

        viewModel.quickRecordBedtimePlus15(now = LocalTime.of(23, 0), today = sleepDay.minusDays(1))

        val record = repository.loadRecords().single()
        assertEquals(LocalTime.of(23, 15), record.bedtime)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
        assertEquals("半成品两端补齐后自动计算时长", 465, record.primarySleepMinutes)
    }

    @Test
    fun plus15ReportsFailureViaSnackbar() = runBlocking {
        val repository = InMemorySleepRecordRepository(failOnWrite = true)
        val viewModel = viewModel(repository)

        val events = snackbarEvents(viewModel) { viewModel.quickRecordBedtimePlus15(now = LocalTime.of(23, 0)) }

        assertTrue(events.single().contains("失败"))
    }

    @Test
    fun wakeAnchorCreatesHalfRecordOnNextSleepDayForEveningSet() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        val today = LocalDate.now()

        val events = snackbarEvents(viewModel) { viewModel.recordWakeAnchor(LocalTime.of(7, 0), now = LocalTime.of(23, 0), today = today) }

        val record = repository.loadRecords().single()
        assertEquals("晚间设闹钟归属次日睡眠日", today.plusDays(1), record.date)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
        assertNull("尚无入睡数据时应保持半成品记录", record.bedtime)
        assertNull(record.primarySleepMinutes)
        assertTrue(events.single().contains("07:00"))
    }

    @Test
    fun wakeAnchorCompletesBedtimeOnlyHalfRecordWithCrossMidnightDuration() = runBlocking {
        val sleepDay = LocalDate.now().plusDays(1)
        val bedtimeOnly = SleepRecord(sleepDay, LocalTime.of(23, 15), null, null, 0)
        val repository = InMemorySleepRecordRepository(listOf(bedtimeOnly))
        val viewModel = viewModel(repository)

        snackbarEvents(viewModel) { viewModel.recordWakeAnchor(LocalTime.of(7, 0), now = LocalTime.of(23, 30), today = sleepDay.minusDays(1)) }

        val record = repository.loadRecords().single()
        assertEquals(LocalTime.of(23, 15), record.bedtime)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
        assertEquals("23:15 到 07:00 跨午夜 465 分钟", 465, record.primarySleepMinutes)
    }

    @Test
    fun wakeAnchorAttributesToCurrentDayWhenSetAfterMidnight() = runBlocking {
        val repository = InMemorySleepRecordRepository()
        val viewModel = viewModel(repository)
        val today = LocalDate.now()

        viewModel.recordWakeAnchor(LocalTime.of(7, 0), now = LocalTime.of(0, 30), today = today)

        val record = repository.loadRecords().single()
        assertEquals("凌晨（已在睡眠中）设闹钟归属当天", today, record.date)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
    }

    @Test
    fun wakeAnchorKeepsSleepingPlaceholderState() = runBlocking {
        val sleepDay = LocalDate.now().plusDays(1)
        val sleeping = SleepRecord(sleepDay, LocalTime.of(23, 0), LocalTime.of(6, 30), 0, 0)
        val repository = InMemorySleepRecordRepository(listOf(sleeping))
        val viewModel = viewModel(repository)

        viewModel.recordWakeAnchor(LocalTime.of(7, 0), now = LocalTime.of(22, 0), today = sleepDay.minusDays(1))

        val record = repository.loadRecords().single()
        assertEquals("睡眠中状态由醒来确认结束，设闹钟只更新起床端", 0, record.primarySleepMinutes)
        assertEquals(LocalTime.of(7, 0), record.wakeTime)
        assertEquals(LocalTime.of(23, 0), record.bedtime)
    }

    @Test
    fun wakeAnchorReportsFailureViaSnackbar() = runBlocking {
        val repository = InMemorySleepRecordRepository(failOnWrite = true)
        val viewModel = viewModel(repository)

        val events = snackbarEvents(viewModel) { viewModel.recordWakeAnchor(LocalTime.of(7, 0)) }

        assertTrue(events.single().contains("失败"))
    }
}
