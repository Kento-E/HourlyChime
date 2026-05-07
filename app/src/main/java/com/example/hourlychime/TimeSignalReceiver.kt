package com.example.hourlychime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

class TimeSignalReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TimeSignalReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimeSignalScheduler.ACTION_TIME_SIGNAL) return

        // goAsync でメインスレッドをブロックせず、バックグラウンド処理を完了させる
        val result = goAsync()
        Thread {
            try {
                val settings = TimeSignalPrefs.load(context)
                if (!settings.enabled) {
                    return@Thread
                }

                val now = Calendar.getInstance()
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                val isHoliday = settings.skipHolidays && HolidayRepository.isHoliday(context, now)

                if (hour in settings.startHour..settings.endHour &&
                    dayOfWeek in settings.enabledDays &&
                    !isHoliday
                ) {
                    NotificationHelper.postTimeSignal(context, hour)
                    Log.i(TAG, "%02d:00 時報を通知しました".format(hour))
                } else {
                    Log.d(
                        TAG,
                        "条件不一致のためスキップ (hour=$hour, dayOfWeek=$dayOfWeek, isHoliday=$isHoliday)",
                    )
                }

                // キャッシュ期限切れなら祝日データを更新（次回判定に備える）
                if (HolidayRepository.isCacheExpired(context)) {
                    HolidayRepository.refreshSync(context)
                }

                // 次のアラームをスケジュール
                TimeSignalScheduler.schedule(context)
            } catch (e: Exception) {
                Log.e(TAG, "時報処理中にエラーが発生しました", e)
            } finally {
                result.finish()
            }
        }.start()
    }
}
