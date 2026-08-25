package com.example.sleepcycle.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepcycle.model.SleepQuality
import com.example.sleepcycle.model.SleepRecommendation
import com.example.sleepcycle.ui.theme.LocalSleepGradients

/**
 * 带有微光质感、呼吸动效与高质感 Badge 的 RecommendationCard
 */
@Composable
fun ModernRecommendationCard(
    recommendation: SleepRecommendation,
    mode: CalculationMode,
    onSetAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecommended = recommendation.isRecommended
    val gradients = LocalSleepGradients.current

    // 按压动效
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press_anim"
    )

    val borderModifier = if (isRecommended) {
        Modifier.border(
            width = 1.5.dp,
            brush = gradients.glowBorderBrush,
            shape = RoundedCornerShape(20.dp)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(20.dp)
        )
    }

    val backgroundBrush = if (isRecommended) {
        gradients.recommendedCardBrush
    } else {
        gradients.cardBackgroundBrush
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .shadow(
                elevation = if (isRecommended) 10.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isRecommended) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent
            )
            .then(borderModifier)
            .background(
                brush = backgroundBrush,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 时间与推荐徽章
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = recommendation.formattedTargetTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRecommended) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        letterSpacing = (-0.2).sp
                    )

                    if (isRecommended) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(gradients.primaryGradientBrush)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "最佳推荐",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 周期数与质量标签
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${recommendation.cycleCount} 个完整周期 (${recommendation.totalHoursText})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ModernQualityChip(quality = recommendation.quality)
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 描述小字
                Text(
                    text = recommendation.quality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 最佳唤醒窗口副标签 (targetTime ± 15 分钟)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "唤醒窗口 ${recommendation.wakeWindowText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 设闹钟 / 设提醒 按钮
            if (isRecommended) {
                Button(
                    onClick = onSetAlarm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (mode == CalculationMode.PLAN_WAKEUP) Icons.Default.NotificationsActive else Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (mode == CalculationMode.PLAN_WAKEUP) "设提醒" else "设闹钟",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onSetAlarm,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (mode == CalculationMode.PLAN_WAKEUP) Icons.Default.NotificationsActive else Icons.Default.Alarm,
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
}

/**
 * 带有微渐变与呼吸高光的质量标签
 */
@Composable
fun ModernQualityChip(quality: SleepQuality) {
    val (bgColor, textColor) = when (quality) {
        SleepQuality.EXCELLENT -> Color(0xFF10B981).copy(alpha = 0.16f) to Color(0xFF10B981)
        SleepQuality.OPTIMAL -> Color(0xFF38BDF8).copy(alpha = 0.16f) to Color(0xFF0284C7)
        SleepQuality.SUFFICIENT -> Color(0xFFF59E0B).copy(alpha = 0.16f) to Color(0xFFD97706)
        SleepQuality.RECHARGE -> Color(0xFFA855F7).copy(alpha = 0.16f) to Color(0xFF9333EA)
        SleepQuality.NAP -> Color(0xFF06B6D4).copy(alpha = 0.16f) to Color(0xFF0891B2)
        SleepQuality.SHORT -> Color(0xFFEF4444).copy(alpha = 0.16f) to Color(0xFFDC2626)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = quality.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
