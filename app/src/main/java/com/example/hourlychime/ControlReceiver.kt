package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

/** 外部アプリ（Galaxyルーチン、Taskerなど）からの制御Intentを受け取り、時報設定を更新する。 */
class ControlReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val current = TimeSignalPrefs.load(context)
    val updated =
            when (action) {
              ACTION_TOGGLE_ENABLED -> handleToggleEnabled(intent, current)
              ACTION_SET_TIME_RANGE -> handleSetTimeRange(intent, current)
              ACTION_SET_ENABLED_DAYS -> handleSetEnabledDays(intent, current)
              ACTION_SET_BLUETOOTH_FILTER -> handleSetBluetoothFilter(intent, current)
              ACTION_SET_SKIP_HOLIDAYS -> handleSetSkipHolidays(intent, current)
              else -> {
                Log.w(TAG, "未対応アクション: $action")
                return
              }
            }

    if (updated == current) {
      Log.i(TAG, "設定変更なし: action=$action")
      return
    }

    TimeSignalPrefs.save(context, updated)
    ScheduleCache.invalidate(context)
    TimeSignalScheduler.schedule(context)
    Log.i(TAG, "外部操作で設定を反映: action=$action")
  }

  private fun handleToggleEnabled(intent: Intent, current: TimeSignalSettings): TimeSignalSettings {
    val state = intent.getStringExtra(EXTRA_STATE)?.trim()?.lowercase()
    val newEnabled =
            when (state) {
              "on", "true", "enable", "enabled", "1" -> true
              "off", "false", "disable", "disabled", "0" -> false
              null -> !current.enabled
              else -> {
                Log.w(TAG, "state の値が不正です: $state")
                return current
              }
            }
    return current.copy(enabled = newEnabled)
  }

  private fun handleSetTimeRange(intent: Intent, current: TimeSignalSettings): TimeSignalSettings {
    val startHour = intent.getIntExtra(EXTRA_START_HOUR, current.startHour)
    val endHour = intent.getIntExtra(EXTRA_END_HOUR, current.endHour)
    if (startHour !in 0..23 || endHour !in 0..23 || startHour > endHour) {
      Log.w(TAG, "時間帯が不正です: start=$startHour end=$endHour")
      return current
    }
    return current.copy(startHour = startHour, endHour = endHour)
  }

  private fun handleSetEnabledDays(
          intent: Intent,
          current: TimeSignalSettings
  ): TimeSignalSettings {
    val raw = intent.getStringExtra(EXTRA_DAYS)?.trim().orEmpty()
    if (raw.isEmpty()) {
      Log.w(TAG, "days が空です")
      return current
    }

    val days =
            raw.split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
                    .toSet()

    if (days.isEmpty()) {
      Log.w(TAG, "有効な曜日が指定されていません: $raw")
      return current
    }

    return current.copy(enabledDays = days)
  }

  private fun handleSetBluetoothFilter(
          intent: Intent,
          current: TimeSignalSettings
  ): TimeSignalSettings {
    val enabled = intent.getBooleanExtra(EXTRA_ENABLED, current.bluetoothFilterEnabled)

    val addressesRaw = intent.getStringExtra(EXTRA_TARGET_ADDRESSES)
    val addresses =
            if (addressesRaw.isNullOrBlank()) {
              current.bluetoothTargetDevices
            } else {
              addressesRaw
                      .split(',')
                      .map { it.trim().uppercase() }
                      .filter { it.isNotEmpty() }
                      .toSet()
            }

    if (enabled && addresses.isEmpty()) {
      Log.w(TAG, "Bluetoothフィルター有効化時に対象アドレスが空です")
      return current
    }

    return current.copy(
            bluetoothFilterEnabled = enabled,
            bluetoothTargetDevices = addresses,
    )
  }

  private fun handleSetSkipHolidays(
          intent: Intent,
          current: TimeSignalSettings
  ): TimeSignalSettings {
    val skip = intent.getBooleanExtra(EXTRA_SKIP, current.skipHolidays)
    return current.copy(skipHolidays = skip)
  }

  companion object {
    private const val TAG = "ControlReceiver"

    const val ACTION_TOGGLE_ENABLED = "com.example.hourlychime.ACTION_TOGGLE_ENABLED"
    const val ACTION_SET_TIME_RANGE = "com.example.hourlychime.ACTION_SET_TIME_RANGE"
    const val ACTION_SET_ENABLED_DAYS = "com.example.hourlychime.ACTION_SET_ENABLED_DAYS"
    const val ACTION_SET_BLUETOOTH_FILTER = "com.example.hourlychime.ACTION_SET_BLUETOOTH_FILTER"
    const val ACTION_SET_SKIP_HOLIDAYS = "com.example.hourlychime.ACTION_SET_SKIP_HOLIDAYS"

    const val EXTRA_STATE = "state"
    const val EXTRA_START_HOUR = "start_hour"
    const val EXTRA_END_HOUR = "end_hour"
    const val EXTRA_DAYS = "days"
    const val EXTRA_ENABLED = "enabled"
    const val EXTRA_TARGET_ADDRESSES = "target_addresses"
    const val EXTRA_SKIP = "skip"
  }
}
