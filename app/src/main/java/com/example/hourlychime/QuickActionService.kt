package com.example.hourlychime

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Galaxy Routine の「アプリの操作を実行」に対応するサービス。
 * Samsung Galaxyデバイスの「ルーチン」→「アプリの操作を実行」で利用可能な
 * クイックアクションを定義する。
 */
class QuickActionService : Service() {
    companion object {
        private const val TAG = "QuickActionService"

        const val ACTION_TOGGLE_TIME_SIGNAL =
            "com.example.hourlychime.ACTION_TOGGLE_TIME_SIGNAL"
        const val ACTION_TURN_ON = "com.example.hourlychime.ACTION_TURN_ON"
        const val ACTION_TURN_OFF = "com.example.hourlychime.ACTION_TURN_OFF"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action=$action")

        when (action) {
            ACTION_TOGGLE_TIME_SIGNAL -> {
                handleToggleTimeSignal()
            }
            ACTION_TURN_ON -> {
                handleTurnOn()
            }
            ACTION_TURN_OFF -> {
                handleTurnOff()
            }
            else -> {
                Log.w(TAG, "Unknown action: $action")
            }
        }

        return START_NOT_STICKY
    }

    private fun handleToggleTimeSignal() {
        val settings = TimeSignalPrefs.load(this)
        val newSettings = settings.copy(enabled = !settings.enabled)
        TimeSignalPrefs.save(this, newSettings)
        ScheduleCache.invalidate(this)
        TimeSignalScheduler.schedule(this)

        Log.i(TAG, "時報を${if (newSettings.enabled) "ON" else "OFF"}に切り替えました")
    }

    private fun handleTurnOn() {
        val settings = TimeSignalPrefs.load(this)
        if (!settings.enabled) {
            val newSettings = settings.copy(enabled = true)
            TimeSignalPrefs.save(this, newSettings)
            ScheduleCache.invalidate(this)
            TimeSignalScheduler.schedule(this)
            Log.i(TAG, "時報をONに設定しました")
        }
    }

    private fun handleTurnOff() {
        val settings = TimeSignalPrefs.load(this)
        if (settings.enabled) {
            val newSettings = settings.copy(enabled = false)
            TimeSignalPrefs.save(this, newSettings)
            ScheduleCache.invalidate(this)
            TimeSignalScheduler.schedule(this)
            Log.i(TAG, "時報をOFFに設定しました")
        }
    }
}
