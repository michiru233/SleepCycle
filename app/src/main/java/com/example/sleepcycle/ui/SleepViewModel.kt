package com.example.sleepcycle.ui

import androidx.lifecycle.ViewModel
import com.example.sleepcycle.model.SleepCalculator
import com.example.sleepcycle.model.SleepRecommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime

/**
 * 睡眠计算应用三大工作模式
 */
enum class CalculationMode(val title: String, val subtitle: String) {
    SLEEP_NOW("我现在就睡", "根据当前时间与入睡潜伏期推荐醒来时间"),
    PLAN_BEDTIME("我计划入睡", "指定就寝时间，正推推荐起床闹钟"),
    PLAN_WAKEUP("我计划起床", "指定期望起床时间，倒推建议上床时间")
}

data class SleepUiState(
    val selectedMode: CalculationMode = CalculationMode.SLEEP_NOW,
    val selectedTime: LocalTime = LocalTime.of(23, 0),
    val latencyMinutes: Int = SleepCalculator.DEFAULT_FALL_ASLEEP_LATENCY_MINUTES,
    val recommendations: List<SleepRecommendation> = emptyList()
)

class SleepViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

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
        _uiState.update { it.copy(latencyMinutes = latency.coerceIn(0, 60)) }
        recalculate()
    }

    fun refreshCurrentTime() {
        if (_uiState.value.selectedMode == CalculationMode.SLEEP_NOW) {
            _uiState.update { it.copy(selectedTime = LocalTime.now()) }
            recalculate()
        }
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
}
