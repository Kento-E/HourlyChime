package com.example.hourlychime

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.bluetooth.BluetoothDevice
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSignalScreen(
        onRequestExactAlarmPermission: () -> Unit,
        onRequestBluetoothPermission: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(TimeSignalPrefs.load(context)) }
    val nextChimeCalculatingText = stringResource(R.string.next_chime_calculating)
    var nextChimeText by remember(nextChimeCalculatingText) {
        mutableStateOf(nextChimeCalculatingText)
    }
    var hasExactAlarmPermission by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    var hasBluetoothPermission by remember {
        mutableStateOf(BluetoothHelper.hasBluetoothConnectPermission(context))
    }
    var bondedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    // 起動時に祝日キャッシュを更新し、次回時報テキストを計算する
    LaunchedEffect(Unit) {
        if (HolidayRepository.isCacheExpired(context)) {
            withContext(Dispatchers.IO) { HolidayRepository.refreshSync(context) }
        }
        nextChimeText = calcNextChimeText(context, settings)
        hasBluetoothPermission = BluetoothHelper.hasBluetoothConnectPermission(context)
        bondedDevices = BluetoothHelper.getBondedDevices(context)
    }

    val saveAndReschedule: (TimeSignalSettings) -> Unit = { s ->
        settings = s
        TimeSignalPrefs.save(context, s)
        // 設定変更時はスケジュールキャッシュを無効化（次回スケジュール時に再計算）
        ScheduleCache.invalidate(context)
        TimeSignalScheduler.schedule(context)
        scope.launch(Dispatchers.IO) {
            val text = calcNextChimeText(context, s)
            withContext(Dispatchers.Main) { nextChimeText = text }
        }
    }

    Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 正確なアラーム権限バナー（Android 12+ で未許可のとき表示）
        if (!hasExactAlarmPermission) {
            Card(
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                            text = stringResource(R.string.exact_alarm_permission_required),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                    )
                    TextButton(
                            onClick = {
                                onRequestExactAlarmPermission()
                                hasExactAlarmPermission = canScheduleExactAlarms(context)
                            }
                    ) { Text(stringResource(R.string.go_to_settings)) }
                }
            }
        }

        // マスター ON/OFF
        Card {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.hourly_chime), style = MaterialTheme.typography.titleMedium)
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(stringResource(R.string.weekday), style = MaterialTheme.typography.titleMedium)
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                        text =
                                stringResource(
                                        R.string.time_range_format,
                                        "%02d:00".format(settings.startHour),
                                        "%02d:00".format(settings.endHour),
                                ),
                        style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))

                Text(
                        stringResource(R.string.start_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                        value = settings.startHour.toFloat(),
                        onValueChange = { v ->
                            val h = v.roundToInt().coerceAtMost(settings.endHour)
                            saveAndReschedule(settings.copy(startHour = h))
                        },
                        valueRange = 0f..23f,
                        steps = 22, // 0..23 の 24 段階
                )

                Text(
                        stringResource(R.string.stop_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                        value = settings.endHour.toFloat(),
                        onValueChange = { v ->
                            val h = v.roundToInt().coerceAtLeast(settings.startHour)
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.skip_holidays), style = MaterialTheme.typography.titleMedium)
                    Text(
                            text = stringResource(R.string.skip_holidays_description),
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

        // Bluetooth フィルター設定
        BluetoothFilterCard(
                settings = settings,
                hasBluetoothPermission = hasBluetoothPermission,
                bondedDevices = bondedDevices,
                onRequestBluetoothPermission = {
                    onRequestBluetoothPermission()
                    hasBluetoothPermission = BluetoothHelper.hasBluetoothConnectPermission(context)
                    bondedDevices = BluetoothHelper.getBondedDevices(context)
                },
                onSettingsChanged = saveAndReschedule,
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothFilterCard(
        settings: TimeSignalSettings,
        hasBluetoothPermission: Boolean,
        bondedDevices: List<BluetoothDevice>,
        onRequestBluetoothPermission: () -> Unit,
        onSettingsChanged: (TimeSignalSettings) -> Unit,
) {
    Card {
        Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            stringResource(R.string.bluetooth_only_when_connected),
                            style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                            text = stringResource(R.string.bluetooth_only_when_connected_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                        checked = settings.bluetoothFilterEnabled,
                        onCheckedChange = {
                            onSettingsChanged(settings.copy(bluetoothFilterEnabled = it))
                        },
                )
            }

            if (settings.bluetoothFilterEnabled) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                if (!hasBluetoothPermission) {
                    // 権限なしバナー
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                                text = stringResource(R.string.bluetooth_permission_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onRequestBluetoothPermission) {
                            Text(stringResource(R.string.allow_permission))
                        }
                    }
                } else if (bondedDevices.isEmpty()) {
                    Text(
                            text = stringResource(R.string.no_paired_bluetooth_devices),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                            text = stringResource(R.string.select_target_device),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    bondedDevices.forEach { device ->
                        val address = device.address
                        val name = runCatching { device.name }.getOrNull() ?: address
                        val isSelected = address in settings.bluetoothTargetDevices
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        val newDevices =
                                                if (checked) {
                                                    settings.bluetoothTargetDevices + address
                                                } else {
                                                    settings.bluetoothTargetDevices - address
                                                }
                                        onSettingsChanged(
                                                settings.copy(bluetoothTargetDevices = newDevices),
                                        )
                                    },
                            )
                            Column {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                        address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
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
    val daySunday = stringResource(R.string.day_sunday)
    val dayMonday = stringResource(R.string.day_monday)
    val dayTuesday = stringResource(R.string.day_tuesday)
    val dayWednesday = stringResource(R.string.day_wednesday)
    val dayThursday = stringResource(R.string.day_thursday)
    val dayFriday = stringResource(R.string.day_friday)
    val daySaturday = stringResource(R.string.day_saturday)
    val days =
            remember(
                    daySunday,
                    dayMonday,
                    dayTuesday,
                    dayWednesday,
                    dayThursday,
                    dayFriday,
                    daySaturday,
            ) {
                listOf(
                        Calendar.SUNDAY to daySunday,
                        Calendar.MONDAY to dayMonday,
                        Calendar.TUESDAY to dayTuesday,
                        Calendar.WEDNESDAY to dayWednesday,
                        Calendar.THURSDAY to dayThursday,
                        Calendar.FRIDAY to dayFriday,
                        Calendar.SATURDAY to daySaturday,
                )
            }
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEach { (dayConst, label) ->
            val selected = dayConst in enabledDays
            FilterChip(
                    selected = selected,
                    onClick = {
                        val newDays =
                                if (selected) enabledDays - dayConst else enabledDays + dayConst
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
    if (!settings.enabled) return context.getString(R.string.next_chime_off)
    val nextMs =
            TimeSignalScheduler.findNextChimeTime(context, settings)
                    ?: return context.getString(R.string.next_chime_not_found)
    val cal = Calendar.getInstance().apply { timeInMillis = nextMs }
    val datetime =
            SimpleDateFormat(
                            context.getString(R.string.next_chime_datetime_format),
                            Locale.JAPAN,
                    )
                    .format(cal.time)
    return context.getString(R.string.next_chime_format, datetime)
}
