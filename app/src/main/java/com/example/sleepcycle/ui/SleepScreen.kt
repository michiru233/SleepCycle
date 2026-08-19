package com.example.sleepcycle.ui

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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepcycle.alarm.AlarmIntentManager
import com.example.sleepcycle.model.SleepRecommendation
import com.example.sleepcycle.ui.theme.LocalSleepGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gradients = LocalSleepGradients.current

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
                        }
                    )
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
                    title = "避开深睡强行唤醒",
                    content = "若在深睡期被闹钟强行打断，会引发严重的“睡眠惯性”，导致大脑长时间昏沉迟钝。"
                )
                ScienceTipItem(
                    title = "推荐 5~6 个周期",
                    content = "成年人每晚通常需要 5~6 个完整周期（7.5~9 小时）以完成深层次身体修复与记忆巩固。"
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
