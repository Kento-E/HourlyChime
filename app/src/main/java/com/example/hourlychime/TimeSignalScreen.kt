package com.example.hourlychime

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSignalScreen(onRequestExactAlarmPermission: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(TimeSignalPrefs.load(context)) }
    var nextChimeText by remember { mutableStateOf("計算中...") }
    var hasExactAlarmPermission by remember {
        mutableStateOf(canScheduleExactAlarms(context))
    }

    // 起動時に祝日キャッシュを更新し、次回時報テキストを計算する
    LaunchedEffect(Unit) {
        if (HolidayRepository.isCacheExpired(context)) {
            withContext(Dispatchers.IO) { HolidayRepository.refreshSync(context) }
        }
        nextChimeText = calcNextChimeText(context, settings)
    }

    val saveAndReschedule: (TimeSignalSettings) -> Unit = { s ->
        settings = s
        TimeSignalPrefs.save(context, s)
        TimeSignalScheduler.schedule(context)
        scope.launch(Dispatchers.IO) {
            val text = calcNextChimeText(context, s)
            withContext(Dispatchers.Main) { nextChimeText = text }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 正確なアラーム権限バナー（Android 12+ で未許可のとき表示）
        if (!hasExactAlarmPermission) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "正確なアラームの許可が必要です",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        onRequestExactAlarmPermission()
                        hasExactAlarmPermission = canScheduleExactAlarms(context)
                    }) {
                        Text("設定へ")
                    }
                }
            }
        }

        // マスター ON/OFF
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("時報", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = nextChimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { saveAndReschedule(settings.copy(enabled = it)) },
                )
            }
        }

        // 曜日設定
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("曜日", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                DayOfWeekSelector(
                    enabledDays = settings.enabledDays,
                    onDaysChanged = { saveAndReschedule(settings.copy(enabledDays = it)) },
                )
            }
        }

        // 時刻範囲設定
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("時刻範囲", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%02d:00 〜 %02d:00".format(settings.startHour, settings.endHour),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    "開始時刻",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = settings.startHour.toFloat(),
                    onValueChange = { v ->
                        val h = v.toInt().coerceAtMost(settings.endHour)
                        saveAndReschedule(settings.copy(startHour = h))
                    },
                    valueRange = 0f..23f,
                    steps = 22, // 0..23 の 24 段階
                )

                Text(
                    "終了時刻",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = settings.endHour.toFloat(),
                    onValueChange = { v ->
                        val h = v.toInt().coerceAtLeast(settings.startHour)
                        saveAndReschedule(settings.copy(endHour = h))
                    },
                    valueRange = 0f..23f,
                    steps = 22,
                )
            }
        }

        // 祝日スキップ設定
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("祝日はスキップ", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "国民の祝日には時報を鳴らさない",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.skipHolidays,
                    onCheckedChange = { saveAndReschedule(settings.copy(skipHolidays = it)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfWeekSelector(
    enabledDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit,
) {
    val days = listOf(
        Calendar.SUNDAY to "日",
        Calendar.MONDAY to "月",
        Calendar.TUESDAY to "火",
        Calendar.WEDNESDAY to "水",
        Calendar.THURSDAY to "木",
        Calendar.FRIDAY to "金",
        Calendar.SATURDAY to "土",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEach { (dayConst, label) ->
            val selected = dayConst in enabledDays
            FilterChip(
                selected = selected,
                onClick = {
                    val newDays = if (selected) enabledDays - dayConst else enabledDays + dayConst
                    onDaysChanged(newDays)
                },
                label = { Text(label) },
            )
        }
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else {
        true
    }
}

private fun calcNextChimeText(context: Context, settings: TimeSignalSettings): String {
    if (!settings.enabled) return "オフ"
    val nextMs = TimeSignalScheduler.findNextChimeTime(context, settings) ?: return "設定なし（7日以内に該当なし）"
    val cal = Calendar.getInstance().apply { timeInMillis = nextMs }
    return "次の時報: " + SimpleDateFormat("M/d (E) HH:00", Locale.JAPAN).format(cal.time)
}
