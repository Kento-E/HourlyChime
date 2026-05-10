package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("BootReceiver", "端末起動を検知しました。時報スケジュールを復元します")

        // 起動時にスケジュールキャッシュを復元（省電力化）
        ScheduleCache.restoreFromPrefs(context)
        TimeSignalScheduler.schedule(context)
    }
}
