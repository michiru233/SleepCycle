package com.example.sleepcycle.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepcycle.alarm.AlarmIntentManager
import com.example.sleepcycle.model.NapType
import com.example.sleepcycle.ui.theme.LocalSleepGradients
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** The six discoverable secondary areas plus the focused calculation home. */
enum class SleepDestination(val label: String) {
    HOME("睡眠计算"),
    RECORDS("睡眠记录"),
    ANALYSIS("睡眠分析"),
    CHRONOTYPE("时间型与光照"),
    NAPS("小睡工具"),
    KNOWLEDGE("睡眠知识"),
    SETTINGS("设置/关于")
}

private data class DrawerEntry(val destination: SleepDestination, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val drawerEntries = listOf(
    DrawerEntry(SleepDestination.RECORDS, Icons.Default.History),
    DrawerEntry(SleepDestination.ANALYSIS, Icons.Default.ShowChart),
    DrawerEntry(SleepDestination.CHRONOTYPE, Icons.Default.WbSunny),
    DrawerEntry(SleepDestination.NAPS, Icons.Default.Timer),
    DrawerEntry(SleepDestination.KNOWLEDGE, Icons.Default.Quiz),
    DrawerEntry(SleepDestination.SETTINGS, Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gradients = LocalSleepGradients.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var destination by rememberSaveable { mutableStateOf(SleepDestination.HOME) }

    LaunchedEffect(viewModel) {
        viewModel.updateEvents.collectLatest { event ->
            when (event) {
                is UpdateEvent.UpToDate -> Toast.makeText(context, "当前已是最新版本 (v${SleepViewModel.CURRENT_APP_VERSION})", Toast.LENGTH_SHORT).show()
                is UpdateEvent.Error -> Toast.makeText(context, "检查更新失败: ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (drawerState.isOpen) {
        BackHandler { scope.launch { drawerState.close() } }
    }

    val updateState = uiState.updateUiState
    if (updateState is UpdateUiState.HasUpdate) {
        UpdateDialog(
            releaseInfo = updateState.releaseInfo,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onDownload = {
                openBrowserUrl(context, updateState.releaseInfo.downloadUrl)
                viewModel.dismissUpdateDialog()
            }
        )
    }

    if (uiState.showCoffeeNapPrompt) {
        CoffeeNapDialog(
            onConfirm = { viewModel.confirmCoffeeNap() },
            onDismiss = { viewModel.dismissCoffeeNapPrompt() }
        )
    }

    LaunchedEffect(uiState.napAlarmRequest) {
        val request = uiState.napAlarmRequest ?: return@LaunchedEffect
        AlarmIntentManager.setAlarm(context, request.targetTime, "SleepCycle 小睡提醒 (${request.napType.label})")
        viewModel.markNapAlarmSet()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("SleepCycle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                Text("睡眠工具", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                NavigationDrawerItem(
                    label = { Text(SleepDestination.HOME.label) },
                    selected = destination == SleepDestination.HOME,
                    onClick = { destination = SleepDestination.HOME; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.NightsStay, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                drawerEntries.forEach { entry ->
                    NavigationDrawerItem(
                        label = { Text(entry.destination.label) },
                        selected = destination == entry.destination,
                        onClick = { destination = entry.destination; scope.launch { drawerState.close() } },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize().background(gradients.backgroundBrush)) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "打开导航菜单")
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(gradients.primaryGradientBrush), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.NightsStay, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("SleepCycle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                                    Text(destination.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = Color.Transparent
            ) { innerPadding ->
                when (destination) {
                    SleepDestination.HOME -> HomeContent(uiState, viewModel, context, innerPadding)
                    SleepDestination.RECORDS -> RecordsContent(uiState, viewModel, innerPadding)
                    SleepDestination.ANALYSIS -> AnalysisContent(uiState, innerPadding)
                    SleepDestination.CHRONOTYPE -> ChronotypeContent(uiState, viewModel, context, innerPadding)
                    SleepDestination.NAPS -> NapContent(uiState, viewModel, innerPadding)
                    SleepDestination.KNOWLEDGE -> KnowledgeContent(innerPadding)
                    SleepDestination.SETTINGS -> SettingsContent(uiState, viewModel, context, innerPadding)
                }
            }
        }
    }
}

@Composable
private fun PageColumn(innerPadding: PaddingValues, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
private fun HomeContent(state: SleepUiState, viewModel: SleepViewModel, context: android.content.Context, innerPadding: PaddingValues) {
    PageColumn(innerPadding) {
        item { Spacer(Modifier.height(2.dp)); SmoothModeSelector(selectedMode = state.selectedMode, onModeSelected = viewModel::onModeSelected) }
        item { ModernTimeSelectionCard(mode = state.selectedMode, selectedTime = state.selectedTime, latencyMinutes = state.latencyMinutes, onTimePicked = viewModel::onTimeSelected, onLatencyChanged = viewModel::onLatencyChanged, onRefreshTime = viewModel::refreshCurrentTime) }
        item {
            val headerText = when (state.selectedMode) {
                CalculationMode.SLEEP_NOW -> "推荐闹钟时间 (若现在入睡)"
                CalculationMode.PLAN_BEDTIME -> "推荐起床时间 (避开深睡期)"
                CalculationMode.PLAN_WAKEUP -> "建议上床时间 (按时入睡)"
            }
            AnimatedContent(targetState = headerText, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "header_text_anim") { targetHeader ->
                Text(targetHeader, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 4.dp))
            }
        }
        items(state.recommendations, key = { it.cycleCount }) { rec ->
            ModernRecommendationCard(recommendation = rec, mode = state.selectedMode, onSetAlarm = {
                val message = when (state.selectedMode) { CalculationMode.PLAN_WAKEUP -> "睡眠周期提示: 准备上床入睡"; else -> "SleepCycle 浅睡智能唤醒 (${rec.cycleCount}个周期)" }
                AlarmIntentManager.setAlarm(context, rec.targetTime, message)
                viewModel.showSleepInertiaGuidance()
            })
        }
        item { state.wakeUpGuidance?.let { WakeUpGuidanceCard(it) } }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun RecordsContent(state: SleepUiState, viewModel: SleepViewModel, innerPadding: PaddingValues) {
    PageColumn(innerPadding) {
        item { SleepRecordSection(state = state, onDateChanged = { viewModel.updateSleepRecordForm(date = it) }, onBedtimeChanged = { viewModel.updateSleepRecordForm(bedtime = it) }, onWakeTimeChanged = { viewModel.updateSleepRecordForm(wakeTime = it) }, onPrimaryChanged = { viewModel.updateSleepRecordForm(primarySleepMinutes = it) }, onNapChanged = { viewModel.updateSleepRecordForm(napMinutes = it) }, onSave = viewModel::saveSleepRecord, onCancelEdit = viewModel::cancelSleepRecordEdit, onDelete = viewModel::deleteSleepRecord, onEdit = viewModel::editSleepRecord, onTargetChanged = viewModel::saveSleepTarget) }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun AnalysisContent(state: SleepUiState, innerPadding: PaddingValues) {
    PageColumn(innerPadding) { item { SleepAnalysisSection(state) }; item { Spacer(Modifier.height(28.dp)) } }
}

@Composable
private fun ChronotypeContent(state: SleepUiState, viewModel: SleepViewModel, context: android.content.Context, innerPadding: PaddingValues) {
    PageColumn(innerPadding) {
        item { ChronotypeCard(profile = state.chronotypeProfile, answers = state.chronotypeAnswers, isEditing = state.isChronotypeEditing, saveState = state.chronotypeSaveState, onEdit = viewModel::beginChronotypeEdit, onCancel = viewModel::cancelChronotypeEdit, onAnswersChanged = viewModel::updateChronotypeAnswers, onSave = viewModel::saveChronotype) }
        item { LightGuidanceCards(morning = state.morningLightGuidance, sunset = state.digitalSunsetGuidance, onMorningAlarm = { AlarmIntentManager.setAlarm(context, it.targetTime, "SleepCycle 晨间户外光提醒") }, onSunsetAlarm = { AlarmIntentManager.setAlarm(context, it.targetTime, "SleepCycle 数字日落提醒") }) }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun NapContent(state: SleepUiState, viewModel: SleepViewModel, innerPadding: PaddingValues) {
    PageColumn(innerPadding) { item { NapPresetCard(selectedNapType = state.selectedNapType, onNapSelected = viewModel::selectNapType) }; item { state.wakeUpGuidance?.let { WakeUpGuidanceCard(it) } }; item { Spacer(Modifier.height(28.dp)) } }
}

@Composable
private fun KnowledgeContent(innerPadding: PaddingValues) {
    PageColumn(innerPadding) { item { ModernScientificNoteCard() }; item { Spacer(Modifier.height(28.dp)) } }
}

@Composable
private fun SettingsContent(state: SleepUiState, viewModel: SleepViewModel, context: android.content.Context, innerPadding: PaddingValues) {
    PageColumn(innerPadding) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Settings, contentDescription = null); Text("设置/关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    Text("会直接影响计算结果的入睡潜伏期已放在首页时间设置中。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = viewModel::checkForUpdates, enabled = state.updateUiState !is UpdateUiState.Checking) {
                        if (state.updateUiState is UpdateUiState.Checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.SystemUpdateAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("检查更新")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Info, contentDescription = null); Text("SleepCycle v${SleepViewModel.CURRENT_APP_VERSION}", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun NapPresetCard(selectedNapType: NapType?, onNapSelected: (NapType) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Text("小睡模式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text("应用内设置系统闹钟", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { NapType.entries.forEach { napType -> FilterChip(selected = selectedNapType == napType, onClick = { onNapSelected(napType) }, label = { Text(if (napType == NapType.COFFEE_NAP) "咖啡 nap" else napType.label, maxLines = 1) }, modifier = Modifier.weight(1f)) } }
            selectedNapType?.let { Text(it.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun CoffeeNapDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, title = { Text("Coffee nap 引导") }, text = { Text("先喝咖啡，20 分钟后咖啡因起效时刚好醒来。咖啡因敏感或临近夜间时请谨慎；个体差异，仅供参考。") }, confirmButton = { TextButton(onClick = onConfirm) { Text("喝咖啡并设 20 分钟") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun WakeUpGuidanceCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)); Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("醒后缓冲提示", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer); Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
    }
}

@Composable
fun ModernScientificNoteCard(modifier: Modifier = Modifier) {
    val gradients = LocalSleepGradients.current
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).background(gradients.cardBackgroundBrush, RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }; Text("睡眠周期科学小贴士", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScienceTipItem("90 分钟周期交替", "人类正常睡眠由 90 分钟的周期循环构成（浅睡、深睡与快速眼动期）。在周期交界处的浅睡期唤醒能让人感到神清气爽。")
                ScienceTipItem("高效午睡：10 或 20 分钟", "Brooks & Lack（2006）研究发现，10 分钟小睡的提神性价比较高；约 20 分钟可用于短时恢复专注。个体差异，仅供参考。")
                ScienceTipItem("Coffee nap：咖啡因与小睡配合", "先喝咖啡再小睡 20 分钟，是一种让咖啡因起效时恰好醒来的引导方式；咖啡因敏感者或临近夜间请谨慎。")
                ScienceTipItem("睡眠惰性：为何睡够仍昏沉", "即使睡够 8 小时，若从深睡阶段被唤醒，也可能出现睡眠惰性；醒后约 15–60 分钟认知可能未完全恢复，晨光与轻度活动有助于过渡。")
                ScienceTipItem("推荐 5~6 个夜间周期", "成年人每晚通常建议 5~6 个完整周期（7.5~9 小时）以完成深层次身体修复与记忆巩固。")
            }
        }
    }
}

@Composable
private fun ScienceTipItem(title: String, content: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp); Column { Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text(content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp) } }
}
