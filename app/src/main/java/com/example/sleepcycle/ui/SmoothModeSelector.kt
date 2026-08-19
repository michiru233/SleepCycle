package com.example.sleepcycle.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepcycle.ui.theme.LocalSleepGradients

/**
 * 带有平滑胶囊指示器动画的 ModeSelector
 */
@Composable
fun SmoothModeSelector(
    selectedMode: CalculationMode,
    onModeSelected: (CalculationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = CalculationMode.values()
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

    val animatedFraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mode_indicator_fraction"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val itemWidth = totalWidth / modes.size

        // 浮动发光胶囊背景指示器
        Box(
            modifier = Modifier
                .offset(x = itemWidth * animatedFraction)
                .width(itemWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(LocalSleepGradients.current.primaryGradientBrush)
        )

        // 标签项
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEachIndexed { index, mode ->
                val isSelected = mode == selectedMode
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onModeSelected(mode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
