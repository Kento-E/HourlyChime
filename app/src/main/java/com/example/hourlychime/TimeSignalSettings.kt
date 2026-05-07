package com.example.hourlychime

import android.content.Context
import java.util.Calendar

/**
 * 時報の動作設定。
 *
 * @param enabled 時報機能のON/OFF
 * @param enabledDays 時報を鳴らす曜日（Calendar.SUNDAY〜Calendar.SATURDAY）
 * @param startHour 時報を鳴らし始める時刻（0〜23）
 * @param endHour 時報を鳴らす最終時刻（0〜23、startHour以上）
 * @param skipHolidays trueの場合、国民の祝日には時報を鳴らさない
 */
data class TimeSignalSettings(
    val enabled: Boolean = false,
    val enabledDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
    ),
    val startHour: Int = 9,
    val endHour: Int = 17,
    val skipHolidays: Boolean = true,
)

object TimeSignalPrefs {
    private const val PREF_NAME = "time_signal_prefs"

    fun load(context: Context): TimeSignalSettings {
        val p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val daysStr = p.getString("enabled_days", null)
        val days = if (daysStr.isNullOrEmpty()) {
            setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
            )
        } else {
            daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        }
        return TimeSignalSettings(
            enabled = p.getBoolean("enabled", false),
            enabledDays = days,
            startHour = p.getInt("start_hour", 9),
            endHour = p.getInt("end_hour", 17),
            skipHolidays = p.getBoolean("skip_holidays", true),
        )
    }

    fun save(context: Context, settings: TimeSignalSettings) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", settings.enabled)
            .putString("enabled_days", settings.enabledDays.joinToString(","))
            .putInt("start_hour", settings.startHour)
            .putInt("end_hour", settings.endHour)
            .putBoolean("skip_holidays", settings.skipHolidays)
            .apply()
    }
}
