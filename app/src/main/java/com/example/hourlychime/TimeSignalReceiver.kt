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

        // 通知の発行と次のアラームスケジューリングはメインスレッドで高速に実行する。
        // これによりシステムが保持するウェイクロックの保持時間を最小化し省バッテリーを実現する。
        try {
            val settings = TimeSignalPrefs.load(context)
            if (settings.enabled) {
                val now = Calendar.getInstance()
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                val isHoliday = settings.skipHolidays && HolidayRepository.isHoliday(context, now)

                val isBluetoothOk = if (settings.bluetoothFilterEnabled) {
                    BluetoothHelper.isAnyTargetDeviceConnected(
                        context,
                        settings.bluetoothTargetDevices,
                    )
                } else {
                    true
                }

                if (hour in settings.startHour..settings.endHour &&
                    dayOfWeek in settings.enabledDays &&
                    !isHoliday &&
                    isBluetoothOk
                ) {
                    NotificationHelper.postTimeSignal(context, hour)
                    Log.i(TAG, "%02d:00 時報を通知しました".format(hour))
                } else {
                    Log.d(
                        TAG,
                        "条件不一致のためスキップ (hour=$hour, dayOfWeek=$dayOfWeek, isHoliday=$isHoliday, isBluetoothOk=$isBluetoothOk)",
                    )
                }
            }

            // 次のアラームをスケジュール（AlarmManager への登録のみなので高速）
            TimeSignalScheduler.schedule(context)
        } catch (e: Exception) {
            Log.e(TAG, "時報処理中にエラーが発生しました", e)
        }

        // 祝日キャッシュの有効期限が切れている場合のみネットワーク更新を行う（7日に1回程度）。
        // goAsync() でウェイクロックを延長しながらバックグラウンドスレッドで実行する。
        if (HolidayRepository.isCacheExpired(context)) {
            val pendingResult = goAsync()
            Thread {
                try {
                    HolidayRepository.refreshSync(context)
                } catch (e: Exception) {
                    Log.e(TAG, "祝日データの更新に失敗しました", e)
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }
}
