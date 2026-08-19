package com.example.sleepcycle.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sleepcycle.data.InMemorySleepPreferencesRepository
import com.example.sleepcycle.data.SharedPreferencesSleepPreferencesRepository
import com.example.sleepcycle.data.SleepPreferencesRepository
import com.example.sleepcycle.model.SleepCalculator
import com.example.sleepcycle.model.SleepRecommendation
import com.example.sleepcycle.update.ReleaseInfo
import com.example.sleepcycle.update.UpdateCheckResult
import com.example.sleepcycle.update.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * 睡眠计算应用三大工作模式
 */
enum class CalculationMode(val title: String, val subtitle: String) {
    SLEEP_NOW("我现在就睡", "根据当前时间与入睡潜伏期推荐醒来时间"),
    PLAN_BEDTIME("我计划入睡", "指定就寝时间，正推推荐起床闹钟"),
    PLAN_WAKEUP("我计划起床", "指定期望起床时间，倒推建议上床时间")
}

/**
 * 检查更新状态定义
 */
sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class HasUpdate(val releaseInfo: ReleaseInfo) : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

/**
 * 单次更新事件通知（用于 Toast / Snackbar 反馈）
 */
sealed class UpdateEvent {
    data object UpToDate : UpdateEvent()
    data class Error(val message: String) : UpdateEvent()
}

data class SleepUiState(
    val selectedMode: CalculationMode = CalculationMode.SLEEP_NOW,
    val selectedTime: LocalTime = LocalTime.of(23, 0),
    val latencyMinutes: Int = SleepCalculator.DEFAULT_FALL_ASLEEP_LATENCY_MINUTES,
    val recommendations: List<SleepRecommendation> = emptyList(),
    val updateUiState: UpdateUiState = UpdateUiState.Idle
)

class SleepViewModel(
    private val preferencesRepository: SleepPreferencesRepository = InMemorySleepPreferencesRepository(),
    private val updateChecker: UpdateChecker = UpdateChecker(),
    private val appVersionName: String = CURRENT_APP_VERSION,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        SleepUiState(
            latencyMinutes = preferencesRepository.getLatencyMinutes()
        )
    )
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    private val _updateEvents = MutableSharedFlow<UpdateEvent>()
    val updateEvents: SharedFlow<UpdateEvent> = _updateEvents.asSharedFlow()

    init {
        recalculate()
    }

    fun onModeSelected(mode: CalculationMode) {
        _uiState.update { current ->
            val defaultTime = when (mode) {
                CalculationMode.SLEEP_NOW -> LocalTime.now()
                CalculationMode.PLAN_BEDTIME -> LocalTime.of(23, 0)
                CalculationMode.PLAN_WAKEUP -> LocalTime.of(7, 0)
            }
            current.copy(
                selectedMode = mode,
                selectedTime = defaultTime
            )
        }
        recalculate()
    }

    fun onTimeSelected(time: LocalTime) {
        _uiState.update { it.copy(selectedTime = time) }
        recalculate()
    }

    fun onLatencyChanged(latency: Int) {
        val clampedLatency = latency.coerceIn(0, 60)
        preferencesRepository.setLatencyMinutes(clampedLatency)
        _uiState.update { it.copy(latencyMinutes = clampedLatency) }
        recalculate()
    }

    fun refreshCurrentTime() {
        if (_uiState.value.selectedMode == CalculationMode.SLEEP_NOW) {
            _uiState.update { it.copy(selectedTime = LocalTime.now()) }
            recalculate()
        }
    }

    /**
     * 触发检查更新
     */
    fun checkForUpdates() {
        if (_uiState.value.updateUiState is UpdateUiState.Checking) {
            return
        }

        _uiState.update { it.copy(updateUiState = UpdateUiState.Checking) }

        scope.launch {
            when (val result = updateChecker.checkForUpdate(appVersionName)) {
                is UpdateCheckResult.HasUpdate -> {
                    _uiState.update { it.copy(updateUiState = UpdateUiState.HasUpdate(result.releaseInfo)) }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.update { it.copy(updateUiState = UpdateUiState.UpToDate) }
                    _updateEvents.emit(UpdateEvent.UpToDate)
                }
                is UpdateCheckResult.Error -> {
                    _uiState.update { it.copy(updateUiState = UpdateUiState.Error(result.message)) }
                    _updateEvents.emit(UpdateEvent.Error(result.message))
                }
            }
        }
    }

    /**
     * 关闭或重置更新弹窗/状态
     */
    fun dismissUpdateDialog() {
        _uiState.update { it.copy(updateUiState = UpdateUiState.Idle) }
    }

    private fun recalculate() {
        _uiState.update { state ->
            val results = when (state.selectedMode) {
                CalculationMode.SLEEP_NOW -> {
                    val now = LocalTime.now()
                    SleepCalculator.calculateSleepNowWakeUpTimes(now, state.latencyMinutes)
                }
                CalculationMode.PLAN_BEDTIME -> {
                    SleepCalculator.calculateWakeUpTimes(state.selectedTime, state.latencyMinutes)
                }
                CalculationMode.PLAN_WAKEUP -> {
                    SleepCalculator.calculateBedtimes(state.selectedTime, state.latencyMinutes)
                }
            }
            state.copy(recommendations = results)
        }
    }

    companion object {
        const val CURRENT_APP_VERSION = "1.2.0"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val repository = SharedPreferencesSleepPreferencesRepository.create(application)
                val versionName = try {
                    val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                    pInfo.versionName ?: CURRENT_APP_VERSION
                } catch (_: Exception) {
                    CURRENT_APP_VERSION
                }
                SleepViewModel(
                    preferencesRepository = repository,
                    updateChecker = UpdateChecker(),
                    appVersionName = versionName
                )
            }
        }
    }
}
