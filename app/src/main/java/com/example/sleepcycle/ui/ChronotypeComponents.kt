package com.example.sleepcycle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sleepcycle.model.ChronotypeAnswers
import com.example.sleepcycle.model.ChronotypeCategory
import com.example.sleepcycle.model.LightGuidance
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val CHRONOTYPE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ChronotypeCard(
    profile: com.example.sleepcycle.model.ChronotypeProfile?,
    answers: ChronotypeAnswers,
    isEditing: Boolean,
    saveState: ChronotypeSaveState,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onAnswersChanged: (ChronotypeAnswers) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("我的时间型", style = MaterialTheme.typography.titleMedium)
            if (isEditing) {
                Text("简化版自我了解测评，不是医学诊断。", style = MaterialTheme.typography.bodySmall)
                ChronotypeTimeRow("工作日上床", answers.workdayBedtime) { onAnswersChanged(answers.copy(workdayBedtime = it)) }
                ChronotypeTimeRow("工作日入睡", answers.workdaySleepTime) { onAnswersChanged(answers.copy(workdaySleepTime = it)) }
                ChronotypeTimeRow("工作日起床", answers.workdayWakeTime) { onAnswersChanged(answers.copy(workdayWakeTime = it)) }
                ChronotypeTimeRow("休息日上床", answers.freeDayBedtime) { onAnswersChanged(answers.copy(freeDayBedtime = it)) }
                ChronotypeTimeRow("休息日入睡", answers.freeDaySleepTime) { onAnswersChanged(answers.copy(freeDaySleepTime = it)) }
                ChronotypeTimeRow("休息日起床", answers.freeDayWakeTime) { onAnswersChanged(answers.copy(freeDayWakeTime = it)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("需要闹钟", style = MaterialTheme.typography.bodySmall)
                    listOf(true to "是", false to "否").forEach { (value, label) ->
                        FilterChip(
                            selected = answers.needsAlarm == value,
                            onClick = { onAnswersChanged(answers.copy(needsAlarm = value)) },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave) { Text("保存测评") }
                    OutlinedButton(onClick = onCancel) { Text("取消") }
                }
                if (saveState is ChronotypeSaveState.Error) Text(saveState.message, color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    text = profile?.let { "睡眠中点 ${it.midpointText} · ${it.category.label}" } ?: "尚未完成，当前显示待完善",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text("跨工作日与休息日计算平均睡眠中点；完整量表和临床评估不由本 App 替代。", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onEdit) { Text(if (profile == null) "开始测评" else "重新测评") }
            }
        }
    }
}

@Composable
private fun ChronotypeTimeRow(label: String, time: LocalTime?, onTimeChanged: (LocalTime) -> Unit) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label ${time?.format(CHRONOTYPE_TIME_FORMATTER) ?: "未填写"}", modifier = Modifier.weight(1f))
        OutlinedButton(onClick = {
            val initial = time ?: LocalTime.of(23, 0)
            android.app.TimePickerDialog(
                context,
                { _, hour, minute -> onTimeChanged(LocalTime.of(hour, minute)) },
                initial.hour,
                initial.minute,
                true
            ).show()
        }) { Text(if (time == null) "填写" else "调整") }
    }
}

@Composable
fun LightGuidanceCards(
    morning: LightGuidance.MorningLight?,
    sunset: LightGuidance.DigitalSunset?,
    onMorningAlarm: (LightGuidance.MorningLight) -> Unit,
    onSunsetAlarm: (LightGuidance.DigitalSunset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        morning?.let {
            GuidanceCard(
                title = "晨间户外光",
                icon = "☀",
                body = "${it.windowStart.format(CHRONOTYPE_TIME_FORMATTER)}–${it.windowEnd.format(CHRONOTYPE_TIME_FORMATTER)} · ${it.note}",
                onSetAlarm = { onMorningAlarm(it) }
            )
        }
        sunset?.let {
            GuidanceCard(
                title = "睡前数字日落",
                icon = "◐",
                body = "${it.windowStart.format(CHRONOTYPE_TIME_FORMATTER)}–${it.windowEnd.format(CHRONOTYPE_TIME_FORMATTER)} · ${it.note}",
                onSetAlarm = { onSunsetAlarm(it) }
            )
        }
    }
}

@Composable
private fun GuidanceCard(title: String, icon: String, body: String, onSetAlarm: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$icon  $title", style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onSetAlarm) { Text("设置提醒") }
        }
    }
}
