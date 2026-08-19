package com.example.sleepcycle

import com.example.sleepcycle.data.InMemorySleepPreferencesRepository
import com.example.sleepcycle.model.SleepCalculator
import com.example.sleepcycle.ui.CalculationMode
import com.example.sleepcycle.ui.SleepViewModel
import com.example.sleepcycle.ui.UpdateEvent
import com.example.sleepcycle.ui.UpdateUiState
import com.example.sleepcycle.update.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SleepViewModelTest {

    @Test
    fun testInitialLatencyLoadsFromRepository() {
        // 模拟已保存 25 分钟潜伏期
        val repo = InMemorySleepPreferencesRepository(initialLatency = 25)
        val viewModel = SleepViewModel(preferencesRepository = repo)

        assertEquals(25, viewModel.uiState.value.latencyMinutes)
    }

    @Test
    fun testDefaultLatencyWhenRepositoryHasDefault() {
        val repo = InMemorySleepPreferencesRepository()
        val viewModel = SleepViewModel(preferencesRepository = repo)

        assertEquals(
            SleepCalculator.DEFAULT_FALL_ASLEEP_LATENCY_MINUTES,
            viewModel.uiState.value.latencyMinutes
        )
    }

    @Test
    fun testLatencyChangeSavesToRepositoryAndRecalculates() {
        val repo = InMemorySleepPreferencesRepository(initialLatency = 14)
        val viewModel = SleepViewModel(preferencesRepository = repo)

        // 切换为指定就寝时间 23:00
        viewModel.onModeSelected(CalculationMode.PLAN_BEDTIME)
        viewModel.onTimeSelected(LocalTime.of(23, 0))

        // 5周期原本：23:00 + 14m + 450m = 06:44
        val rec14 = viewModel.uiState.value.recommendations.find { it.cycleCount == 5 }!!
        assertEquals(LocalTime.of(6, 44), rec14.targetTime)

        // 修改潜伏期为 20 分钟
        viewModel.onLatencyChanged(20)

        // 验证内存仓库同步更新
        assertEquals(20, repo.getLatencyMinutes())
        assertEquals(20, viewModel.uiState.value.latencyMinutes)

        // 5周期更新后：23:00 + 20m + 450m = 06:50
        val rec20 = viewModel.uiState.value.recommendations.find { it.cycleCount == 5 }!!
        assertEquals(LocalTime.of(6, 50), rec20.targetTime)
    }

    @Test
    fun testRecreatingViewModelRestoresLatency() {
        val repo = InMemorySleepPreferencesRepository(initialLatency = 14)
        
        // 第一次启动并修改
        val viewModel1 = SleepViewModel(preferencesRepository = repo)
        viewModel1.onLatencyChanged(30)
        assertEquals(30, repo.getLatencyMinutes())

        // 退出重进（重新创建 ViewModel）
        val viewModel2 = SleepViewModel(preferencesRepository = repo)
        assertEquals(30, viewModel2.uiState.value.latencyMinutes)
    }

    @Test
    fun testCheckForUpdatesHasUpdateFlow() = runBlocking {
        val stubChecker = UpdateChecker(
            httpFetcher = {
                """
                {
                  "tag_name": "v1.2.0",
                  "name": "v1.2.0 - 新特性",
                  "body": "更新说明详情",
                  "published_at": "2026-08-19T10:00:00Z",
                  "html_url": "https://github.com/michiru233/SleepCycle/releases/tag/v1.2.0",
                  "assets": []
                }
                """.trimIndent()
            },
            ioDispatcher = Dispatchers.Unconfined
        )

        val viewModel = SleepViewModel(
            updateChecker = stubChecker,
            appVersionName = "1.1.1",
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        assertEquals(UpdateUiState.Idle, viewModel.uiState.value.updateUiState)

        viewModel.checkForUpdates()

        val state = viewModel.uiState.value.updateUiState
        assertTrue(state is UpdateUiState.HasUpdate)
        assertEquals("1.2.0", (state as UpdateUiState.HasUpdate).releaseInfo.versionName)

        // 测试 dismissUpdateDialog
        viewModel.dismissUpdateDialog()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value.updateUiState)
    }

    @Test
    fun testCheckForUpdatesUpToDateEmitsEvent() = runBlocking {
        val stubChecker = UpdateChecker(
            httpFetcher = {
                """
                {
                  "tag_name": "v1.1.1",
                  "name": "v1.1.1",
                  "body": "当前已是最新",
                  "published_at": "2026-08-19T10:00:00Z",
                  "html_url": "https://github.com/michiru233/SleepCycle/releases/tag/v1.1.1",
                  "assets": []
                }
                """.trimIndent()
            },
            ioDispatcher = Dispatchers.Unconfined
        )

        val viewModel = SleepViewModel(
            updateChecker = stubChecker,
            appVersionName = "1.1.1",
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        var receivedEvent: UpdateEvent? = null
        val job = launch(Dispatchers.Unconfined) {
            receivedEvent = viewModel.updateEvents.first()
        }

        viewModel.checkForUpdates()

        assertEquals(UpdateUiState.UpToDate, viewModel.uiState.value.updateUiState)
        assertEquals(UpdateEvent.UpToDate, receivedEvent)
        job.cancel()
    }

    @Test
    fun testCheckForUpdatesErrorEmitsEvent() = runBlocking {
        val errorChecker = UpdateChecker(
            httpFetcher = { throw java.io.IOException("无法连接至 GitHub") },
            ioDispatcher = Dispatchers.Unconfined
        )

        val viewModel = SleepViewModel(
            updateChecker = errorChecker,
            appVersionName = "1.1.1",
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        var receivedEvent: UpdateEvent? = null
        val job = launch(Dispatchers.Unconfined) {
            receivedEvent = viewModel.updateEvents.first()
        }

        viewModel.checkForUpdates()

        val state = viewModel.uiState.value.updateUiState
        assertTrue(state is UpdateUiState.Error)
        assertTrue((state as UpdateUiState.Error).message.contains("无法连接至 GitHub"))
        assertTrue(receivedEvent is UpdateEvent.Error)
        job.cancel()
    }
}
