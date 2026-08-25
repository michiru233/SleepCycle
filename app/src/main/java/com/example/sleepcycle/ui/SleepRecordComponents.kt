package com.example.sleepcycle.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleepcycle.model.SocialJetLagResult
import com.example.sleepcycle.model.SleepRecord
import com.example.sleepcycle.model.SleepSettings
import com.example.sleepcycle.model.TwoProcessPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val RECORD_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun SleepRecordSection(
    state: SleepUiState,
    onDateChanged: (LocalDate) -> Unit,
    onBedtimeChanged: (LocalTime) -> Unit,
    onWakeTimeChanged: (LocalTime) -> Unit,
    onPrimaryChanged: (Int) -> Unit,
    onNapChanged: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: (LocalDate) -> Unit,
    onEdit: (LocalDate) -> Unit,
    onTargetChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("睡眠记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("手动记录昨夜主睡眠和午睡，日期按入睡所在日保存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = {
                DatePickerDialog(context, { _, year, month, day -> onDateChanged(LocalDate.of(year, month + 1, day)) }, state.recordDate.year, state.recordDate.monthValue - 1, state.recordDate.dayOfMonth).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("日期：${state.recordDate}") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, hour, minute -> onBedtimeChanged(LocalTime.of(hour, minute)) }, state.recordBedtime.hour, state.recordBedtime.minute, true).show()
                }, modifier = Modifier.weight(1f)) { Text("入睡 ${state.recordBedtime.format(RECORD_TIME_FORMATTER)}") }
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, hour, minute -> onWakeTimeChanged(LocalTime.of(hour, minute)) }, state.recordWakeTime.hour, state.recordWakeTime.minute, true).show()
                }, modifier = Modifier.weight(1f)) { Text("起床 ${state.recordWakeTime.format(RECORD_TIME_FORMATTER)}") }
            }
            Text("主睡眠时间段：${SleepRecord.durationBetween(state.recordBedtime, state.recordWakeTime)} 分钟", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("实际主睡眠", modifier = Modifier.weight(1f))
                var primaryText by remember(state.recordPrimarySleepMinutes) { mutableStateOf(state.recordPrimarySleepMinutes.toString()) }
                androidx.compose.material3.OutlinedTextField(value = primaryText, onValueChange = { value ->
                    primaryText = value.filter(Char::isDigit).take(4)
                    onPrimaryChanged(primaryText.toIntOrNull() ?: 0)
                }, label = { Text("分钟") }, singleLine = true, modifier = Modifier.width(120.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("午睡", modifier = Modifier.weight(1f))
                var napText by remember(state.recordNapMinutes) { mutableStateOf(state.recordNapMinutes.toString()) }
                androidx.compose.material3.OutlinedTextField(value = napText, onValueChange = { value ->
                    napText = value.filter(Char::isDigit).take(4)
                    onNapChanged(napText.toIntOrNull() ?: 0)
                }, label = { Text("分钟") }, singleLine = true, modifier = Modifier.width(120.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text(if (state.recordSaveState is SleepRecordSaveState.Saving) "保存中" else "保存记录") }
                IconButton(onClick = { onDelete(state.recordDate) }) { Icon(Icons.Default.Delete, contentDescription = "删除记录") }
            }
            when (val saveState = state.recordSaveState) {
                is SleepRecordSaveState.Error -> Text(saveState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                SleepRecordSaveState.Saved -> Text("已保存并重新读取记录", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                else -> Unit
            }
            Text("睡眠目标：${state.sleepSettings.targetMinutes / 60}小时${state.sleepSettings.targetMinutes % 60}分", style = MaterialTheme.typography.labelLarge)
            Slider(value = state.sleepSettings.targetMinutes.toFloat(), onValueChange = { value ->
                val target = (value.roundToInt() / 15) * 15
                onTargetChanged(target.coerceIn(SleepSettings().targetMinutes - 120, SleepSettings().targetMinutes + 120))
            }, valueRange = 360f..600f, steps = 15)
            if (state.sleepRecords.isNotEmpty()) {
                state.sleepRecords.take(3).forEach { record ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("${record.date}  ${record.primarySleepMinutes} 分钟", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onEdit(record.date) }) { Icon(Icons.Default.Edit, contentDescription = "编辑 ${record.date}") }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepAnalysisSection(
    state: SleepUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("睡眠分析（模型参考）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("最近 14 个完整本地日期 · 已记录 ${state.sleepGapSummary.recordedDays} 天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("平均主睡眠：${state.sleepGapSummary.averagePrimarySleepMinutes} 分钟", style = MaterialTheme.typography.bodyMedium)
                Text("估算睡眠缺口：${state.sleepGapSummary.estimatedGapMinutes} 分钟", style = MaterialTheme.typography.bodyMedium)
                when (val jetLag = state.socialJetLag) {
                    SocialJetLagResult.Incomplete -> Text("社会时差：数据不足（工作日和休息日各需至少 2 条）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is SocialJetLagResult.Complete -> Text("社会时差：${"%.1f".format(jetLag.differenceHours)} 小时（中点差，估算）", style = MaterialTheme.typography.bodyMedium)
                }
                Text("缺口和社会时差是行为参考，不代表医疗诊断或因果关系。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TwoProcessChart(state.twoProcessPoints)
    }
}

@Composable
private fun TwoProcessChart(points: List<TwoProcessPoint>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("今日能量 / 睡眠倾向（模型估算）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("综合睡眠倾向同时参考稳态压力与昼夜节律警觉信号；仅作行为参考。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (points.isEmpty()) {
                Text("记录后显示 24 小时曲线", style = MaterialTheme.typography.bodySmall)
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = size.width * index / (points.size - 1).coerceAtLeast(1)
                        val y = size.height * (1f - point.sleepTendency.toFloat())
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = Color(0xFF5E6AD2), style = Stroke(width = 4f))
                    points.forEachIndexed { index, point ->
                        if (index % 4 == 0) {
                            val x = size.width * index / (points.size - 1).coerceAtLeast(1)
                            val y = size.height * (1f - point.sleepTendency.toFloat())
                            drawCircle(if (point.isAsleep) Color(0xFF7B61A8) else Color(0xFF35A77C), radius = 3f, center = Offset(x, y))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(points.first().clockTime.format(RECORD_TIME_FORMATTER), style = MaterialTheme.typography.labelSmall)
                    Text(points[points.size / 2].clockTime.format(RECORD_TIME_FORMATTER), style = MaterialTheme.typography.labelSmall)
                    Text(points.last().clockTime.format(RECORD_TIME_FORMATTER), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
