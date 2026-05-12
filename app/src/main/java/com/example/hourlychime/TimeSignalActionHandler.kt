package com.example.hourlychime

import android.content.Context
import android.util.Log

object TimeSignalActionHandler {
    private const val TAG = "TimeSignalActionHandler"

    const val ACTION_TOGGLE_TIME_SIGNAL = "com.example.hourlychime.ACTION_TOGGLE_TIME_SIGNAL"
    const val ACTION_TURN_ON = "com.example.hourlychime.ACTION_TURN_ON"
    const val ACTION_TURN_OFF = "com.example.hourlychime.ACTION_TURN_OFF"

    fun handle(context: Context, action: String?): Boolean {
        return when (action) {
            ACTION_TOGGLE_TIME_SIGNAL -> {
                toggle(context)
                true
            }
            ACTION_TURN_ON -> {
                setEnabled(context, true)
                true
            }
            ACTION_TURN_OFF -> {
                setEnabled(context, false)
                true
            }
            else -> false
        }
    }

    private fun toggle(context: Context) {
        val settings = TimeSignalPrefs.load(context)
        applyEnabled(context, settings.enabled.not())
    }

    private fun setEnabled(context: Context, enabled: Boolean) {
        val settings = TimeSignalPrefs.load(context)
        if (settings.enabled == enabled) {
            Log.i(TAG, "時報は既に${if (enabled) "ON" else "OFF"}です")
            return
        }
        applyEnabled(context, enabled)
    }

    private fun applyEnabled(context: Context, enabled: Boolean) {
        val settings = TimeSignalPrefs.load(context)
        val newSettings = settings.copy(enabled = enabled)
        TimeSignalPrefs.save(context, newSettings)
        ScheduleCache.invalidate(context)
        TimeSignalScheduler.schedule(context)

        Log.i(TAG, "時報を${if (enabled) "ON" else "OFF"}に設定しました")
    }
}