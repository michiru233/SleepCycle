package com.example.sleepcycle.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gradients = LocalSleepGradients.current

    // 监听一次性更新事件（Toast 反馈）
    LaunchedEffect(viewModel) {
        viewModel.updateEvents.collectLatest { event ->
            when (event) {
                is UpdateEvent.UpToDate -> {
                    Toast.makeText(context, "当前已是最新版本 (v${SleepViewModel.CURRENT_APP_VERSION})", Toast.LENGTH_SHORT).show()
                }
                is UpdateEvent.Error -> {
                    Toast.makeText(context, "检查更新失败: ${event.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 发现新版本时展示弹窗
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
        val message = "SleepCycle 小睡提醒 (${request.napType.label})"
        AlarmIntentManager.setAlarm(context, request.targetTime, message)
        viewModel.markNapAlarmSet()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.backgroundBrush)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(gradients.primaryGradientBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SleepCycle",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "智能睡眠周期唤醒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.checkForUpdates() },
                            enabled = uiState.updateUiState !is UpdateUiState.Checking
                        ) {
                            if (uiState.updateUiState is UpdateUiState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdateAlt,
                                    contentDescription = "检查更新",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 模式选择器
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    SmoothModeSelector(
                        selectedMode = uiState.selectedMode,
                        onModeSelected = { viewModel.onModeSelected(it) }
                    )
                }

                item {
                    NapPresetCard(
                        selectedNapType = uiState.selectedNapType,
                        onNapSelected = { viewModel.selectNapType(it) }
                    )
                }

                item {
                    SleepRecordSection(
                        state = uiState,
                        onDateChanged = { viewModel.updateSleepRecordForm(date = it) },
                        onBedtimeChanged = { viewModel.updateSleepRecordForm(bedtime = it) },
                        onWakeTimeChanged = { viewModel.updateSleepRecordForm(wakeTime = it) },
                        onPrimaryChanged = { viewModel.updateSleepRecordForm(primarySleepMinutes = it) },
                        onNapChanged = { viewModel.updateSleepRecordForm(napMinutes = it) },
                        onSave = { viewModel.saveSleepRecord() },
                        onDelete = { viewModel.deleteSleepRecord(it) },
                        onEdit = { viewModel.editSleepRecord(it) },
                        onTargetChanged = { viewModel.saveSleepTarget(it) }
                    )
                }

                item {
                    SleepAnalysisSection(state = uiState)
                }

                item {
                    ChronotypeCard(
                        profile = uiState.chronotypeProfile,
                        answers = uiState.chronotypeAnswers,
                        isEditing = uiState.isChronotypeEditing,
                        saveState = uiState.chronotypeSaveState,
                        onEdit = { viewModel.beginChronotypeEdit() },
                        onCancel = { viewModel.cancelChronotypeEdit() },
                        onAnswersChanged = { viewModel.updateChronotypeAnswers(it) },
                        onSave = { viewModel.saveChronotype() }
                    )
                }

                item {
                    LightGuidanceCards(
                        morning = uiState.morningLightGuidance,
                        sunset = uiState.digitalSunsetGuidance,
                        onMorningAlarm = { guidance ->
                            AlarmIntentManager.setAlarm(context, guidance.targetTime, "SleepCycle 晨间户外光提醒")
                        },
                        onSunsetAlarm = { guidance ->
                            AlarmIntentManager.setAlarm(context, guidance.targetTime, "SleepCycle 数字日落提醒")
                        }
                    )
                }

                // 2. 时间选择卡片
                item {
                    ModernTimeSelectionCard(
                        mode = uiState.selectedMode,
                        selectedTime = uiState.selectedTime,
                        latencyMinutes = uiState.latencyMinutes,
                        onTimePicked = { viewModel.onTimeSelected(it) },
                        onLatencyChanged = { viewModel.onLatencyChanged(it) },
                        onRefreshTime = { viewModel.refreshCurrentTime() }
                    )
                }

                // 3. 结果标题（带平滑过渡）
                item {
                    val headerText = when (uiState.selectedMode) {
                        CalculationMode.SLEEP_NOW -> "推荐闹钟时间 (若现在入睡)"
                        CalculationMode.PLAN_BEDTIME -> "推荐起床时间 (避开深睡期)"
                        CalculationMode.PLAN_WAKEUP -> "建议上床时间 (按时入睡)"
                    }
                    AnimatedContent(
                        targetState = headerText,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "header_text_anim"
                    ) { targetHeader ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = targetHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // 4. 推荐周期卡片列表
                items(
                    items = uiState.recommendations,
                    key = { it.cycleCount }
                ) { rec ->
                    ModernRecommendationCard(
                        recommendation = rec,
                        mode = uiState.selectedMode,
                        onSetAlarm = {
                            val message = when (uiState.selectedMode) {
                                CalculationMode.PLAN_WAKEUP -> "睡眠周期提示: 准备上床入睡"
                                else -> "SleepCycle 浅睡智能唤醒 (${rec.cycleCount}个周期)"
                            }
                            AlarmIntentManager.setAlarm(context, rec.targetTime, message)
                            viewModel.showSleepInertiaGuidance()
                        }
                    )
                }

                item {
                    uiState.wakeUpGuidance?.let { guidance ->
                        WakeUpGuidanceCard(text = guidance)
                    }
                }

                // 5. 科学知识卡片与底部边距
                item {
                    ModernScientificNoteCard()
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun NapPresetCard(
    selectedNapType: NapType?,
    onNapSelected: (NapType) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "小睡模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "应用内设置系统闹钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NapType.entries.forEach { napType ->
                    FilterChip(
                        selected = selectedNapType == napType,
                        onClick = { onNapSelected(napType) },
                        label = {
                            Text(
                                text = when (napType) {
                                    NapType.COFFEE_NAP -> "咖啡 nap"
                                    else -> napType.label
                                },
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            selectedNapType?.let { napType ->
                Text(
                    text = napType.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CoffeeNapDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Coffee nap 引导") },
        text = {
            Text("先喝咖啡，20 分钟后咖啡因起效时刚好醒来。咖啡因敏感或临近夜间时请谨慎；个体差异，仅供参考。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("喝咖啡并设 20 分钟") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun WakeUpGuidanceCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "醒后缓冲提示",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 带有柔和毛玻璃/磨砂质感的睡眠科学知识卡片
 */
@Composable
fun ModernScientificNoteCard(modifier: Modifier = Modifier) {
    val gradients = LocalSleepGradients.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                brush = gradients.cardBackgroundBrush,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "睡眠周期科学小贴士",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScienceTipItem(
                    title = "90 分钟周期交替",
                    content = "人类正常睡眠由 90 分钟的周期循环构成（浅睡、深睡与快速眼动期）。在周期交界处的浅睡期唤醒能让人感到神清气爽。"
                )
                ScienceTipItem(
                    title = "高效午睡：10 或 20 分钟",
                    content = "Brooks & Lack（2006）研究发现，10 分钟小睡的提神性价比较高；约 20 分钟可用于短时恢复专注。个体差异，仅供参考。"
                )
                ScienceTipItem(
                    title = "Coffee nap：咖啡因与小睡配合",
                    content = "先喝咖啡再小睡 20 分钟，是一种让咖啡因起效时恰好醒来的引导方式；咖啡因敏感者或临近夜间请谨慎。"
                )
                ScienceTipItem(
                    title = "睡眠惰性：为何睡够仍昏沉",
                    content = "即使睡够 8 小时，若从深睡阶段被唤醒，也可能出现睡眠惰性；醒后约 15–60 分钟认知可能未完全恢复，晨光与轻度活动有助于过渡。"
                )
                ScienceTipItem(
                    title = "推荐 5~6 个夜间周期",
                    content = "成年人每晚通常建议 5~6 个完整周期（7.5~9 小时）以完成深层次身体修复与记忆巩固。"
                )
            }
        }
    }
}

@Composable
private fun ScienceTipItem(
    title: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
