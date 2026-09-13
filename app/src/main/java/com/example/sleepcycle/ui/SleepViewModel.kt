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
import com.example.sleepcycle.data.ChronotypeProfileRepository
import com.example.sleepcycle.data.InMemoryChronotypeProfileRepository
import com.example.sleepcycle.data.RoomChronotypeProfileRepository
import com.example.sleepcycle.data.RoomSleepRecordRepository
import com.example.sleepcycle.data.SleepCycleDatabase
import com.example.sleepcycle.data.SleepRecordRepository
import com.example.sleepcycle.model.ChronotypeAnswers
import com.example.sleepcycle.model.ChronotypeCalculator
import com.example.sleepcycle.model.ChronotypeProfile
import com.example.sleepcycle.model.DEFAULT_SLEEP_TARGET_MINUTES
import com.example.sleepcycle.model.LightGuidance
import com.example.sleepcycle.model.LightGuidanceCalculator
import com.example.sleepcycle.model.SleepCalculator
import com.example.sleepcycle.model.SleepRecommendation
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepSettings
import com.example.sleepcycle.model.SleepStatsCalculator
import com.example.sleepcycle.model.SocialJetLagCalculator
import com.example.sleepcycle.model.SocialJetLagResult
import com.example.sleepcycle.model.TwoProcessModel
import com.example.sleepcycle.model.TwoProcessPoint
import com.example.sleepcycle.model.NapAlarmRequest
import com.example.sleepcycle.model.NapType
import com.example.sleepcycle.model.SLEEP_INERTIA_GUIDANCE
import com.example.sleepcycle.update.ReleaseInfo
import com.example.sleepcycle.update.UpdateCheckResult
import com.example.sleepcycle.update.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
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

/**
 * 快速打卡状态摘要
 */
data class QuickRecordSummary(
    val lastBedtime: LocalTime? = null,
    val lastWakeTime: LocalTime? = null,
    val isSleeping: Boolean = false,
    val statusText: String = "今日尚未打卡睡眠"
)

data class SleepUiState(
    val selectedMode: CalculationMode = CalculationMode.SLEEP_NOW,
    val selectedTime: LocalTime = LocalTime.of(23, 0),
    val latencyMinutes: Int = SleepCalculator.DEFAULT_FALL_ASLEEP_LATENCY_MINUTES,
    val recommendations: List<SleepRecommendation> = emptyList(),
    val updateUiState: UpdateUiState = UpdateUiState.Idle,
    val selectedNapType: NapType? = null,
    val napAlarmRequest: NapAlarmRequest? = null,
    val showCoffeeNapPrompt: Boolean = false,
    val wakeUpGuidance: String? = null,
    val chronotypeProfile: ChronotypeProfile? = null,
    val chronotypeAnswers: ChronotypeAnswers = ChronotypeAnswers(null, null, null, null, null, null, null),
    val isChronotypeEditing: Boolean = false,
    val chronotypeSaveState: ChronotypeSaveState = ChronotypeSaveState.Idle,
    val morningLightGuidance: LightGuidance.MorningLight? = null,
    val digitalSunsetGuidance: LightGuidance.DigitalSunset? = null,
    val sleepRecords: List<SleepRecord> = emptyList(),
    val sleepSettings: SleepSettings = SleepSettings(),
    val sleepGapSummary: com.example.sleepcycle.model.SleepGapSummary = SleepStatsCalculator.summarize(emptyList(), SleepSettings().targetMinutes),
    val socialJetLag: SocialJetLagResult = SocialJetLagResult.Incomplete,
    val twoProcessPoints: List<TwoProcessPoint> = emptyList(),
    val quickRecordSummary: QuickRecordSummary = QuickRecordSummary(),
    val recordDate: LocalDate = LocalDate.now().minusDays(1),
    val recordBedtime: LocalTime = LocalTime.of(23, 0),
    val recordWakeTime: LocalTime = LocalTime.of(7, 0),
    val recordPrimarySleepMinutes: Int = 480,
    val recordNapMinutes: Int = 0,
    val editingSleepRecord: SleepRecordDraft? = null,
    val recordSaveState: SleepRecordSaveState = SleepRecordSaveState.Idle,
    val sleepDataError: String? = null
)

data class SleepRecordDraft(
    val date: LocalDate,
    val bedtime: LocalTime,
    val wakeTime: LocalTime,
    val primarySleepMinutes: Int,
    val napMinutes: Int
)

sealed class SleepRecordSaveState {
    data object Idle : SleepRecordSaveState()
    data object Saving : SleepRecordSaveState()
    data object Saved : SleepRecordSaveState()
    data class Error(val message: String) : SleepRecordSaveState()
}

sealed class ChronotypeSaveState {
    data object Idle : ChronotypeSaveState()
    data object Loading : ChronotypeSaveState()
    data object Saving : ChronotypeSaveState()
    data object Saved : ChronotypeSaveState()
    data class Error(val message: String) : ChronotypeSaveState()
}

sealed class NapEvent {
    data class CoffeeNapPrompt(val napType: NapType) : NapEvent()
}

class SleepViewModel(
    private val preferencesRepository: SleepPreferencesRepository = InMemorySleepPreferencesRepository(),
    private val updateChecker: UpdateChecker = UpdateChecker(),
    private val appVersionName: String = CURRENT_APP_VERSION,
    private val chronotypeRepository: ChronotypeProfileRepository = InMemoryChronotypeProfileRepository(),
    private val sleepRecordRepository: SleepRecordRepository = com.example.sleepcycle.data.InMemorySleepRecordRepository(),
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

    private val _quickRecordEvents = MutableSharedFlow<String>()
    val quickRecordEvents: SharedFlow<String> = _quickRecordEvents.asSharedFlow()

    private val _updateEvents = MutableSharedFlow<UpdateEvent>()
    val updateEvents: SharedFlow<UpdateEvent> = _updateEvents.asSharedFlow()

    private val _napEvents = MutableSharedFlow<NapEvent>()
    val napEvents: SharedFlow<NapEvent> = _napEvents.asSharedFlow()

    init {
        recalculate()
        loadChronotype()
        loadSleepData()
    }

    private fun loadSleepData() {
        scope.launch(Dispatchers.Unconfined) {
            runCatching {
                val settings = sleepRecordRepository.loadSettings()
                val records = sleepRecordRepository.loadRecords()
                settings to records
            }.onSuccess { (settings, records) ->
                _uiState.update { it.withSleepData(records, settings, chronotypeProfile = it.chronotypeProfile) }
            }.onFailure { error ->
                _uiState.update { it.copy(sleepDataError = error.message ?: "睡眠记录读取失败") }
            }
        }
    }

    private fun SleepUiState.withSleepData(
        records: List<SleepRecord>,
        settings: SleepSettings,
        chronotypeProfile: ChronotypeProfile? = this.chronotypeProfile
    ): SleepUiState {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        // 查找今日或昨日的最新记录生成快速打卡摘要
        val candidate = records.firstOrNull { it.date == today } ?: records.firstOrNull { it.date == yesterday }
        val summary = if (candidate != null) {
            val isSleeping = candidate.primarySleepMinutes == 0
            val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            val statusText = if (isSleeping) {
                "已记录入睡 ${candidate.bedtime?.format(timeFormatter) ?: "--:--"} · 睡眠中..."
            } else if (!candidate.isComplete) {
                // 半成品记录：联动已写入一端，等待补全
                "已记录起床锚点 · 待补全入睡信息"
            } else {
                val primary = candidate.primarySleepMinutes ?: 0
                val hours = primary / 60
                val mins = primary % 60
                val durationDesc = if (hours > 0 && mins > 0) "${hours}小时${mins}分" else if (hours > 0) "${hours}小时" else "${mins}分钟"
                "入睡 ${candidate.bedtime?.format(timeFormatter) ?: "--:--"} · 醒来 ${candidate.wakeTime?.format(timeFormatter) ?: "--:--"} ($durationDesc)"
            }
            QuickRecordSummary(
                lastBedtime = candidate.bedtime,
                lastWakeTime = if (isSleeping) null else candidate.wakeTime,
                isSleeping = isSleeping,
                statusText = statusText
            )
        } else {
            QuickRecordSummary(statusText = "今日尚未打卡睡眠")
        }

        return copy(
            sleepRecords = records,
            sleepSettings = settings,
            sleepGapSummary = SleepStatsCalculator.summarize(records, settings.targetMinutes),
            socialJetLag = SocialJetLagCalculator.calculate(records),
            twoProcessPoints = TwoProcessModel.generate(records, chronotypeProfile, LocalDateTime.now(), settings.targetMinutes),
            quickRecordSummary = summary,
            sleepDataError = null
        )
    }

    fun updateSleepRecordForm(
        date: LocalDate = _uiState.value.recordDate,
        bedtime: LocalTime = _uiState.value.recordBedtime,
        wakeTime: LocalTime = _uiState.value.recordWakeTime,
        primarySleepMinutes: Int = _uiState.value.recordPrimarySleepMinutes,
        napMinutes: Int = _uiState.value.recordNapMinutes
    ) {
        _uiState.update { it.copy(recordDate = date, recordBedtime = bedtime, recordWakeTime = wakeTime, recordPrimarySleepMinutes = primarySleepMinutes, recordNapMinutes = napMinutes) }
    }

    fun editSleepRecord(date: LocalDate) {
        scope.launch(Dispatchers.Unconfined) {
            runCatching { sleepRecordRepository.getRecord(date) }
                .onSuccess { record ->
                    if (record != null) _uiState.update {
                        // 半成品记录编辑时用默认值补全缺失端（表单可见、可改），保存后覆盖
                        val defaultBedtime = record.bedtime ?: LocalTime.of(23, 0)
                        val defaultWakeTime = record.wakeTime ?: LocalTime.of(7, 0)
                        val defaultPrimary = record.primarySleepMinutes
                            ?: record.clockDurationMinutes
                            ?: DEFAULT_SLEEP_TARGET_MINUTES
                        it.copy(
                            recordDate = record.date,
                            recordBedtime = defaultBedtime,
                            recordWakeTime = defaultWakeTime,
                            recordPrimarySleepMinutes = defaultPrimary,
                            recordNapMinutes = record.napMinutes,
                            editingSleepRecord = SleepRecordDraft(record.date, defaultBedtime, defaultWakeTime, defaultPrimary, record.napMinutes),
                            recordSaveState = SleepRecordSaveState.Idle
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(sleepDataError = error.message ?: "睡眠记录读取失败") } }
        }
    }

    fun cancelSleepRecordEdit() {
        val draft = _uiState.value.editingSleepRecord
        _uiState.update { state ->
            if (draft == null) {
                state.copy(recordSaveState = SleepRecordSaveState.Idle)
            } else {
                state.copy(
                    recordDate = draft.date,
                    recordBedtime = draft.bedtime,
                    recordWakeTime = draft.wakeTime,
                    recordPrimarySleepMinutes = draft.primarySleepMinutes,
                    recordNapMinutes = draft.napMinutes,
                    editingSleepRecord = null,
                    recordSaveState = SleepRecordSaveState.Idle
                )
            }
        }
    }

    fun saveSleepRecord() {
        val state = _uiState.value
        val primaryMinutes = state.recordPrimarySleepMinutes
        val napMinutes = state.recordNapMinutes
        // 睡眠日最晚允许到明天（晚间设闹钟归属次日）
        if (state.recordDate.isAfter(LocalDate.now().plusDays(1)) || primaryMinutes !in 1..SleepRecord.MINUTES_PER_DAY || napMinutes !in 0..SleepRecord.MINUTES_PER_DAY) {
            _uiState.update { it.copy(recordSaveState = SleepRecordSaveState.Error("日期或午睡分钟数无效")) }
            return
        }
        val record = runCatching { SleepRecord(state.recordDate, state.recordBedtime, state.recordWakeTime, primaryMinutes, napMinutes) }
            .getOrElse { error ->
                _uiState.update { it.copy(recordSaveState = SleepRecordSaveState.Error(error.message ?: "睡眠记录无效")) }
                return
            }
        _uiState.update { it.copy(recordSaveState = SleepRecordSaveState.Saving) }
        scope.launch(Dispatchers.Unconfined) {
            runCatching {
                sleepRecordRepository.saveRecord(record)
                sleepRecordRepository.loadRecords()
            }.onSuccess { records ->
                _uiState.update { it.copy(recordSaveState = SleepRecordSaveState.Saved).withSleepData(records, it.sleepSettings).copy(editingSleepRecord = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(recordSaveState = SleepRecordSaveState.Error(error.message ?: "睡眠记录保存失败"), sleepDataError = error.message) }
            }
        }
    }

    fun deleteSleepRecord(date: LocalDate) {
        scope.launch(Dispatchers.Unconfined) {
            runCatching {
                sleepRecordRepository.deleteRecord(date)
                sleepRecordRepository.loadRecords()
            }.onSuccess { records -> _uiState.update { it.withSleepData(records, it.sleepSettings) } }
                .onFailure { error -> _uiState.update { it.copy(sleepDataError = error.message ?: "睡眠记录删除失败") } }
        }
    }

    /**
     * 快速打卡：“我睡了”
     */
    fun quickRecordBedtime(now: LocalTime = LocalTime.now(), today: LocalDate = LocalDate.now()) {
        scope.launch(Dispatchers.Unconfined) {
            val records = sleepRecordRepository.loadRecords()
            val existing = records.firstOrNull { it.date == today }
            val existingWake = existing?.wakeTime
            val placeholderWakeTime = existingWake?.takeIf { it != now } ?: now.plusHours(8)
            val primaryMinutes = if (existingWake != null && existingWake != now && (existing?.primarySleepMinutes ?: 0) > 0) {
                SleepRecord.durationBetween(now, existingWake)
            } else {
                0
            }
            val newRecord = SleepRecord(
                date = today,
                bedtime = now,
                wakeTime = placeholderWakeTime,
                primarySleepMinutes = primaryMinutes,
                napMinutes = existing?.napMinutes ?: 0
            )
            runCatching {
                sleepRecordRepository.saveRecord(newRecord)
                sleepRecordRepository.loadRecords()
            }.onSuccess { updatedRecords ->
                _uiState.update { it.withSleepData(updatedRecords, it.sleepSettings) }
                val timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                _quickRecordEvents.emit("已记录入睡时间 $timeStr")
            }.onFailure { error ->
                _uiState.update { it.copy(sleepDataError = error.message ?: "入睡记录保存失败") }
                _quickRecordEvents.emit("记录入睡失败: ${error.message}")
            }
        }
    }

    /**
     * 快速打卡：“我醒了”
     */
    fun quickRecordWakeTime(now: LocalTime = LocalTime.now(), today: LocalDate = LocalDate.now()) {
        scope.launch(Dispatchers.Unconfined) {
            val records = sleepRecordRepository.loadRecords()
            val yesterday = today.minusDays(1)
            // 优先匹配当天的入睡打卡（如凌晨入睡），若无则匹配昨天的入睡打卡
            val candidate = records.firstOrNull { it.date == today && it.primarySleepMinutes == 0 }
                ?: records.firstOrNull { it.date == yesterday && it.primarySleepMinutes == 0 }
                ?: records.firstOrNull { it.date == yesterday }
                ?: records.firstOrNull { it.date == today }

            val targetDate = candidate?.date ?: yesterday
            val bedtime = candidate?.bedtime ?: LocalTime.of(23, 0)
            val placeholderWakeTime = if (bedtime == now) now.plusMinutes(1) else now
            val primaryMinutes = SleepRecord.durationBetween(bedtime, placeholderWakeTime)
            val updatedRecord = SleepRecord(
                date = targetDate,
                bedtime = bedtime,
                wakeTime = placeholderWakeTime,
                primarySleepMinutes = primaryMinutes,
                napMinutes = candidate?.napMinutes ?: 0
            )

            runCatching {
                sleepRecordRepository.saveRecord(updatedRecord)
                sleepRecordRepository.loadRecords()
            }.onSuccess { updatedRecords ->
                _uiState.update { it.withSleepData(updatedRecords, it.sleepSettings) }
                val timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                val hours = primaryMinutes / 60
                val mins = primaryMinutes % 60
                val durationStr = if (hours > 0 && mins > 0) "${hours}小时${mins}分" else if (hours > 0) "${hours}小时" else "${mins}分钟"
                _quickRecordEvents.emit("已记录醒来 $timeStr (睡眠时长 $durationStr)")
            }.onFailure { error ->
                _uiState.update { it.copy(sleepDataError = error.message ?: "醒来记录保存失败") }
                _quickRecordEvents.emit("记录醒来失败: ${error.message}")
            }
        }
    }

    fun saveSleepTarget(targetMinutes: Int) {
        val settings = runCatching { SleepSettings(targetMinutes) }.getOrElse { error ->
            _uiState.update { it.copy(sleepDataError = error.message ?: "睡眠目标无效") }
            return
        }
        scope.launch(Dispatchers.Unconfined) {
            runCatching {
                sleepRecordRepository.saveSettings(settings)
                sleepRecordRepository.loadSettings()
            }.onSuccess { saved -> _uiState.update { it.withSleepData(it.sleepRecords, saved) } }
                .onFailure { error -> _uiState.update { it.copy(sleepDataError = error.message ?: "睡眠目标保存失败") } }
        }
    }

    private fun loadChronotype() {
        _uiState.update { it.copy(chronotypeSaveState = ChronotypeSaveState.Loading) }
        scope.launch(Dispatchers.Unconfined) {
            runCatching { chronotypeRepository.load() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            chronotypeProfile = profile,
                            chronotypeAnswers = profile?.answers ?: it.chronotypeAnswers,
                            chronotypeSaveState = ChronotypeSaveState.Idle,
                            morningLightGuidance = profile?.answers?.workdayWakeTime?.let { wake -> LightGuidanceCalculator.morningLight(wake, profile) } ?: it.morningLightGuidance,
                            digitalSunsetGuidance = profile?.answers?.workdaySleepTime?.let { sleep -> LightGuidanceCalculator.digitalSunset(sleep, profile) } ?: it.digitalSunsetGuidance
                        ).withSleepData(it.sleepRecords, it.sleepSettings, profile)
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(chronotypeSaveState = ChronotypeSaveState.Error(error.message ?: "时间型档案读取失败")) } }
        }
    }

    fun beginChronotypeEdit() {
        _uiState.update { it.copy(isChronotypeEditing = true, chronotypeSaveState = ChronotypeSaveState.Idle) }
    }

    fun cancelChronotypeEdit() {
        _uiState.update {
            it.copy(
                isChronotypeEditing = false,
                chronotypeAnswers = it.chronotypeProfile?.answers ?: ChronotypeAnswers(null, null, null, null, null, null, null),
                chronotypeSaveState = ChronotypeSaveState.Idle
            )
        }
    }

    fun updateChronotypeAnswers(answers: ChronotypeAnswers) {
        _uiState.update { it.copy(chronotypeAnswers = answers) }
    }

    fun saveChronotype() {
        val answers = _uiState.value.chronotypeAnswers
        val profile = ChronotypeCalculator.calculate(answers)
        _uiState.update { it.copy(chronotypeSaveState = ChronotypeSaveState.Saving) }
        scope.launch(Dispatchers.Unconfined) {
            runCatching { chronotypeRepository.save(profile) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            chronotypeProfile = profile,
                            chronotypeAnswers = profile.answers,
                            isChronotypeEditing = false,
                            chronotypeSaveState = ChronotypeSaveState.Saved,
                            morningLightGuidance = profile.answers.workdayWakeTime?.let { wake -> LightGuidanceCalculator.morningLight(wake, profile) },
                            digitalSunsetGuidance = profile.answers.workdaySleepTime?.let { sleep -> LightGuidanceCalculator.digitalSunset(sleep, profile) }
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(chronotypeSaveState = ChronotypeSaveState.Error(error.message ?: "时间型档案保存失败")) } }
        }
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

    fun selectNapType(napType: NapType) {
        _uiState.update {
            it.copy(
                selectedNapType = napType,
                napAlarmRequest = null,
                showCoffeeNapPrompt = false,
                wakeUpGuidance = null
            )
        }
        if (napType.isCoffeeNap) {
            _uiState.update { it.copy(showCoffeeNapPrompt = true) }
            scope.launch(Dispatchers.Unconfined) { _napEvents.emit(NapEvent.CoffeeNapPrompt(napType)) }
        } else {
            prepareNapAlarm(napType)
        }
    }

    fun confirmCoffeeNap() {
        val napType = _uiState.value.selectedNapType
        if (napType?.isCoffeeNap == true) {
            _uiState.update { it.copy(showCoffeeNapPrompt = false) }
            prepareNapAlarm(napType)
        }
    }

    fun dismissCoffeeNapPrompt() {
        _uiState.update { it.copy(showCoffeeNapPrompt = false, selectedNapType = null) }
    }

    fun clearNapAlarmRequest() {
        _uiState.update { it.copy(napAlarmRequest = null) }
    }

    fun markNapAlarmSet() {
        val napType = _uiState.value.selectedNapType ?: return
        _uiState.update {
            it.copy(
                napAlarmRequest = null,
                wakeUpGuidance = if (napType == NapType.TEN_MINUTES || napType == NapType.TWENTY_MINUTES || napType == NapType.COFFEE_NAP) {
                    SLEEP_INERTIA_GUIDANCE
                } else {
                    napType.wakeUpTip
                }
            )
        }
    }

    fun showSleepInertiaGuidance() {
        _uiState.update { it.copy(wakeUpGuidance = SLEEP_INERTIA_GUIDANCE) }
    }

    private fun prepareNapAlarm(napType: NapType) {
        val targetTime = LocalTime.now().plusMinutes(napType.durationMinutes.toLong())
        _uiState.update {
            it.copy(
                selectedNapType = napType,
                napAlarmRequest = NapAlarmRequest(napType, targetTime),
                showCoffeeNapPrompt = false
            )
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

        scope.launch(Dispatchers.Unconfined) {
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
            state.copy(
                recommendations = results,
                morningLightGuidance = state.chronotypeProfile?.answers?.workdayWakeTime?.let { wake ->
                    LightGuidanceCalculator.morningLight(wake, state.chronotypeProfile)
                } ?: when (state.selectedMode) {
                    CalculationMode.PLAN_WAKEUP -> LightGuidanceCalculator.morningLight(state.selectedTime)
                    else -> LightGuidanceCalculator.morningLight(LocalTime.now())
                },
                digitalSunsetGuidance = state.chronotypeProfile?.answers?.workdaySleepTime?.let { sleep ->
                    LightGuidanceCalculator.digitalSunset(sleep, state.chronotypeProfile)
                } ?: when (state.selectedMode) {
                    CalculationMode.PLAN_BEDTIME -> LightGuidanceCalculator.digitalSunset(state.selectedTime)
                    else -> LightGuidanceCalculator.digitalSunset(LocalTime.of(23, 0))
                }
            )
        }
    }

    companion object {
        const val CURRENT_APP_VERSION = "1.9.5"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val repository = SharedPreferencesSleepPreferencesRepository.create(application)
                val database = androidx.room.Room.databaseBuilder(
                    application,
                    SleepCycleDatabase::class.java,
                    "sleep_cycle.db"
                ).addMigrations(SleepCycleDatabase.MIGRATION_1_2).build()
                val chronotypeRepository = RoomChronotypeProfileRepository(database.chronotypeProfileDao())
                val sleepRecordRepository = RoomSleepRecordRepository(database.sleepRecordDao(), database.sleepSettingsDao())
                val versionName = try {
                    val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                    pInfo.versionName ?: CURRENT_APP_VERSION
                } catch (_: Exception) {
                    CURRENT_APP_VERSION
                }
                SleepViewModel(
                    preferencesRepository = repository,
                    updateChecker = UpdateChecker(),
                    appVersionName = versionName,
                    chronotypeRepository = chronotypeRepository,
                    sleepRecordRepository = sleepRecordRepository
                )
            }
        }
    }
}
