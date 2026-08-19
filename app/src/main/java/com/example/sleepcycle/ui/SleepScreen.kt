package com.example.sleepcycle.ui

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
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
import com.example.sleepcycle.model.SleepQuality
import com.example.sleepcycle.model.SleepRecommendation
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SleepCycle 智能睡眠",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ModeSelector(
                    selectedMode = uiState.selectedMode,
                    onModeSelected = { viewModel.onModeSelected(it) }
                )
            }

            item {
                TimeSelectionCard(
                    mode = uiState.selectedMode,
                    selectedTime = uiState.selectedTime,
                    latencyMinutes = uiState.latencyMinutes,
                    onTimePicked = { viewModel.onTimeSelected(it) },
                    onLatencyChanged = { viewModel.onLatencyChanged(it) },
                    onRefreshTime = { viewModel.refreshCurrentTime() }
                )
            }

            item {
                val headerText = when (uiState.selectedMode) {
                    CalculationMode.SLEEP_NOW -> "推荐闹钟时间 (若现在入睡)"
                    CalculationMode.PLAN_BEDTIME -> "推荐起床时间 (避开深睡期)"
                    CalculationMode.PLAN_WAKEUP -> "建议上床时间 (按时入睡)"
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(uiState.recommendations) { rec ->
                RecommendationCard(
                    recommendation = rec,
                    mode = uiState.selectedMode,
                    onSetAlarm = {
                        val message = when (uiState.selectedMode) {
                            CalculationMode.PLAN_WAKEUP -> "睡眠周期提示: 准备上床入睡"
                            else -> "SleepCycle 浅睡智能唤醒 (${rec.cycleCount}个周期)"
                        }
                        AlarmIntentManager.setAlarm(context, rec.targetTime, message)
                    }
                )
            }

            item {
                ScientificNoteCard()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ModeSelector(
    selectedMode: CalculationMode,
    onModeSelected: (CalculationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        CalculationMode.values().forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = CalculationMode.values().size),
                onClick = { onModeSelected(mode) },
                selected = mode == selectedMode,
                label = {
                    Text(
                        text = mode.title,
                        fontSize = 12.sp,
                        fontWeight = if (mode == selectedMode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
fun TimeSelectionCard(
    mode: CalculationMode,
    selectedTime: LocalTime,
    latencyMinutes: Int,
    onTimePicked: (LocalTime) -> Unit,
    onLatencyChanged: (Int) -> Unit,
    onRefreshTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLatencyDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val label = when (mode) {
                        CalculationMode.SLEEP_NOW -> "当前系统时间"
                        CalculationMode.PLAN_BEDTIME -> "准备就寝时间"
                        CalculationMode.PLAN_WAKEUP -> "计划起床时间"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (mode == CalculationMode.SLEEP_NOW) {
                    FilledTonalButton(
                        onClick = onRefreshTime,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("刷新当前")
                    }
                } else {
                    Button(
                        onClick = {
                            val timePicker = TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onTimePicked(LocalTime.of(hour, minute))
                                },
                                selectedTime.hour,
                                selectedTime.minute,
                                true
                            )
                            timePicker.show()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择时间")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "入睡缓冲潜伏期: ${latencyMinutes} 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showLatencyDialog = true }) {
                    Text("调整")
                }
            }
        }
    }

    if (showLatencyDialog) {
        var sliderValue by remember { mutableStateOf(latencyMinutes.toFloat()) }
        AlertDialog(
            onDismissRequest = { showLatencyDialog = false },
            title = { Text("设置入睡潜伏期") },
            text = {
                Column {
                    Text("从闭眼到进入睡眠状态所需的平均时间（通常为 10~20 分钟）：")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${sliderValue.toInt()} 分钟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..45f,
                        steps = 8
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onLatencyChanged(sliderValue.toInt())
                    showLatencyDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLatencyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun RecommendationCard(
    recommendation: SleepRecommendation,
    mode: CalculationMode,
    onSetAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecommended = recommendation.isRecommended

    val cardBorder = if (isRecommended) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecommended) 4.dp else 1.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recommendation.formattedTargetTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isRecommended) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "推荐",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${recommendation.cycleCount} 个周期 (${recommendation.totalHoursText})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    QualityChip(quality = recommendation.quality)
                }

                Text(
                    text = recommendation.quality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onSetAlarm,
                shape = RoundedCornerShape(12.dp),
                colors = if (isRecommended) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (mode == CalculationMode.PLAN_WAKEUP) "设提醒" else "设闹钟",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun QualityChip(quality: SleepQuality) {
    val (bgColor, textColor) = when (quality) {
        SleepQuality.EXCELLENT -> Color(0xFF2E7D32).copy(alpha = 0.15f) to Color(0xFF2E7D32)
        SleepQuality.OPTIMAL -> Color(0xFF0277BD).copy(alpha = 0.15f) to Color(0xFF0277BD)
        SleepQuality.SUFFICIENT -> Color(0xFFEF6C00).copy(alpha = 0.15f) to Color(0xFFEF6C00)
        SleepQuality.SHORT -> Color(0xFFC62828).copy(alpha = 0.15f) to Color(0xFFC62828)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = quality.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScientificNoteCard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "💡 睡眠周期小知识",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• 正常睡眠由 90 分钟的周期交替循环构成（浅睡、深睡与快速眼动期）。\n" +
                        "• 若在深睡期被闹钟强行唤醒，极易产生睡眠惯性导致全天头晕疲惫。\n" +
                        "• 成年人通常需要 5~6 个完整周期（7.5~9 小时）以达到最佳大脑修复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
