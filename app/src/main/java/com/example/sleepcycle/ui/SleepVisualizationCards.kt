package com.example.sleepcycle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleepcycle.model.DailySleepStat
import com.example.sleepcycle.model.SleepGoalLevel
import com.example.sleepcycle.model.bandAxis
import com.example.sleepcycle.model.bandPositions
import com.example.sleepcycle.model.heatmapRows
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val STAT_LEVEL_ON_TARGET = Color(0xFF10B981)
private val STAT_LEVEL_NEAR_TARGET = Color(0xFFF59E0B)
private val STAT_LEVEL_LARGE_GAP = Color(0xFF94A3B8)
private val STAT_TARGET_LINE = Color(0xFF5E6AD2)
private val STAT_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd")
private val STAT_CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

private fun SleepGoalLevel.barColor(): Color = when (this) {
    SleepGoalLevel.ON_TARGET -> STAT_LEVEL_ON_TARGET
    SleepGoalLevel.NEAR_TARGET -> STAT_LEVEL_NEAR_TARGET
    SleepGoalLevel.LARGE_GAP -> STAT_LEVEL_LARGE_GAP
    SleepGoalLevel.NO_RECORD -> Color.Transparent
}

private fun DailySleepStat.detailText(): String = when {
    goalLevel == SleepGoalLevel.NO_RECORD -> "$date：无完整记录"
    else -> {
        val hours = (primarySleepMinutes ?: 0) / 60
        val mins = (primarySleepMinutes ?: 0) % 60
        val duration = if (hours > 0 && mins > 0) "${hours}小时${mins}分" else if (hours > 0) "${hours}小时" else "${mins}分钟"
        "$date：入睡 ${bedtime?.format(STAT_CLOCK_FORMATTER) ?: "--:--"} · 起床 ${wakeTime?.format(STAT_CLOCK_FORMATTER) ?: "--:--"} · 主睡眠 $duration" +
            if (napMinutes > 0) " · 午睡 ${napMinutes}分钟" else ""
    }
}

/** 可视化区域（睡眠分析页）：三张图共享统计窗口与点选（工单 #10-#12，词汇见 CONTEXT.md） */
@Composable
fun SleepVisualizationSection(
    state: SleepUiState,
    onWindowChange: (Int) -> Unit,
    onDateToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SleepTrendCard(
            stats = state.dailySleepStats,
            targetMinutes = state.sleepSettings.targetMinutes,
            windowDays = state.visualizationWindowDays,
            selectedDate = state.selectedStatDate,
            onWindowChange = onWindowChange,
            onDateToggle = onDateToggle
        )
        SleepBandCard(
            stats = state.dailySleepStats,
            windowDays = state.visualizationWindowDays,
            selectedDate = state.selectedStatDate,
            onDateToggle = onDateToggle
        )
        SleepHeatmapCard(
            stats = state.dailySleepStats,
            selectedDate = state.selectedStatDate,
            onDateToggle = onDateToggle
        )
    }
}

/** 时长趋势柱状图：每日主睡眠柱 + 目标虚线，点选查看当日详情 */
@Composable
fun SleepTrendCard(
    stats: List<DailySleepStat>,
    targetMinutes: Int,
    windowDays: Int,
    selectedDate: LocalDate?,
    onWindowChange: (Int) -> Unit,
    onDateToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("睡眠时长趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 30).forEach { days ->
                    FilterChip(
                        selected = windowDays == days,
                        onClick = { onWindowChange(days) },
                        label = { Text("${days}天") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            if (stats.all { it.goalLevel == SleepGoalLevel.NO_RECORD }) {
                Text(
                    "窗口内暂无完整记录，联动写入后展示趋势",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                TrendCanvas(
                    stats = stats,
                    targetMinutes = targetMinutes,
                    selectedDate = selectedDate,
                    onDateToggle = onDateToggle,
                    modifier = Modifier.fillMaxWidth().height(170.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stats.first().date.format(STAT_TIME_FORMATTER), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stats[stats.size / 2].date.format(STAT_TIME_FORMATTER), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stats.last().date.format(STAT_TIME_FORMATTER), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            selectedDate?.let { date ->
                val stat = stats.firstOrNull { it.date == date }
                if (stat != null) {
                    Text(
                        stat.detailText(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** 作息带状图（工单 #11）：每日一条入睡→起床横带，跨午夜对齐，直观看作息漂移 */
@Composable
fun SleepBandCard(
    stats: List<DailySleepStat>,
    windowDays: Int,
    selectedDate: LocalDate?,
    onDateToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val completeStats = stats.filter { it.bedtime != null && it.wakeTime != null }
    GlassSurface(shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("作息带状图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("最近${windowDays}天", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (completeStats.isEmpty()) {
                Text(
                    "窗口内暂无完整记录，联动写入后展示作息区间",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                val axis = bandAxis(stats)!!
                val positions = bandPositions(axis, stats)
                val axisStart = axis.startMinutes.mod(24 * 60)
                val axisEnd = axis.endMinutes.mod(24 * 60)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatAxisClock(axisStart), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatAxisClock(axisEnd), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val rowHeightPx = 34f
                val canvasHeightDp = ((stats.size * rowHeightPx) / 3f).dp
                val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                val selectionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(canvasHeightDp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .pointerInput(stats, selectedDate) {
                            detectTapGestures { offset ->
                                val index = (offset.y / size.height * stats.size).toInt().coerceIn(0, stats.size - 1)
                                onDateToggle(stats[index].date)
                            }
                        }
                ) {
                    val rowHeight = size.height / stats.size
                    val bandHeight = rowHeight * 0.5f
                    stats.forEachIndexed { index, stat ->
                        val position = positions[index] ?: return@forEachIndexed
                        val rowTop = index * rowHeight + (rowHeight - bandHeight) / 2f
                        drawRoundRect(
                            color = bandColor,
                            topLeft = Offset(position.first * size.width, rowTop),
                            size = Size((position.second - position.first) * size.width, bandHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                        )
                        if (stat.date == selectedDate) {
                            drawRoundRect(
                                color = selectionColor,
                                topLeft = Offset(position.first * size.width - 3f, rowTop - 3f),
                                size = Size((position.second - position.first) * size.width + 6f, bandHeight + 6f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }
            }
            selectedDate?.let { date ->
                val stat = stats.firstOrNull { it.date == date }
                if (stat != null) {
                    Text(
                        stat.detailText(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatAxisClock(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

/** 睡眠热力图（工单 #12）：格子日历按周分行，按达标四档着色，点选查看当日详情 */
@Composable
fun SleepHeatmapCard(
    stats: List<DailySleepStat>,
    selectedDate: LocalDate?,
    onDateToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("睡眠热力图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
            }
            LegendRow()
            HeatmapCanvas(
                stats = stats,
                selectedDate = selectedDate,
                onDateToggle = onDateToggle,
                modifier = Modifier.fillMaxWidth()
            )
            selectedDate?.let { date ->
                val stat = stats.firstOrNull { it.date == date }
                if (stat != null) {
                    Text(
                        stat.detailText(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            SleepGoalLevel.ON_TARGET to "达标",
            SleepGoalLevel.NEAR_TARGET to "接近",
            SleepGoalLevel.LARGE_GAP to "缺口大",
            SleepGoalLevel.NO_RECORD to "无记录"
        ).forEach { (level, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(10.dp)
                        .background(level.barColor().let { if (level == SleepGoalLevel.NO_RECORD) MaterialTheme.colorScheme.surfaceVariant else it }, RoundedCornerShape(3.dp))
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HeatmapCanvas(
    stats: List<DailySleepStat>,
    selectedDate: LocalDate?,
    onDateToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = heatmapRows(stats)
    if (rows.isEmpty()) return
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val selectionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    val cellHeightDp = ((rows.size * 44f) / 3f).dp
    Canvas(
        modifier = modifier
            .height(cellHeightDp)
            .pointerInput(stats, selectedDate) {
                detectTapGestures { offset ->
                    val column = (offset.x / size.width * 7).toInt().coerceIn(0, 6)
                    val row = (offset.y / size.height * rows.size).toInt().coerceIn(0, rows.size - 1)
                    rows[row][column]?.let { onDateToggle(it.date) }
                }
            }
    ) {
        val cellWidth = size.width / 7f
        val cellHeight = size.height / rows.size
        val cellSize = minOf(cellWidth, cellHeight) * 0.72f
        rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                if (cell == null) return@forEachIndexed
                val left = columnIndex * cellWidth + (cellWidth - cellSize) / 2f
                val top = rowIndex * cellHeight + (cellHeight - cellSize) / 2f
                val corner = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                val color = if (cell.goalLevel == SleepGoalLevel.NO_RECORD) emptyColor else cell.goalLevel.barColor()
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(cellSize, cellSize),
                    cornerRadius = corner
                )
                if (cell.date == selectedDate) {
                    drawRoundRect(
                        color = selectionColor,
                        topLeft = Offset(left - 3f, top - 3f),
                        size = Size(cellSize + 6f, cellSize + 6f),
                        cornerRadius = corner,
                        style = Stroke(width = 4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendCanvas(
    stats: List<DailySleepStat>,
    targetMinutes: Int,
    selectedDate: LocalDate?,
    onDateToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColors = stats.map { it.goalLevel.barColor() }
    val selectionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .pointerInput(stats, selectedDate) {
                detectTapGestures { offset ->
                    val index = (offset.x / size.width * stats.size).toInt().coerceIn(0, stats.size - 1)
                    onDateToggle(stats[index].date)
                }
            }
    ) {
        val slot = size.width / stats.size
        val barWidth = slot * 0.62f
        // 纵轴上限取目标与最长一天的较大值再留 15% 余量，保证目标线与柱都在可视区内
        val maxMinutes = maxOf(targetMinutes, stats.maxOf { it.primarySleepMinutes ?: 0 }) * 1.15f
        val drawableHeight = size.height - 12f

        fun yOf(minutes: Int): Float = drawableHeight * (1f - minutes / maxMinutes)

        // 目标虚线
        val targetY = yOf(targetMinutes)
        drawLine(
            color = STAT_TARGET_LINE,
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        )

        stats.forEachIndexed { index, stat ->
            val primary = stat.primarySleepMinutes ?: return@forEachIndexed
            val left = index * slot + (slot - barWidth) / 2f
            val top = yOf(primary)
            drawRoundRect(
                color = barColors[index],
                topLeft = Offset(left, top),
                size = Size(barWidth, drawableHeight - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            if (stat.date == selectedDate) {
                drawRoundRect(
                    color = selectionColor,
                    topLeft = Offset(left - 3f, top - 3f),
                    size = Size(barWidth + 6f, drawableHeight - top + 6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}
