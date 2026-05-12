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
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action=$action")

        if (!TimeSignalActionHandler.handle(this, action)) {
            Log.w(TAG, "Unknown action: $action")
        }

        return START_NOT_STICKY
    }
}
