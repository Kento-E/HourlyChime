package com.example.hourlychime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeSignalScheduler {
    private const val TAG = "TimeSignalScheduler"
    const val ACTION_TIME_SIGNAL = "com.example.hourlychime.ACTION_TIME_SIGNAL"
    private const val REQUEST_CODE = 1001

    /**
     * 設定に基づいて次の時報アラームをスケジュールする。 設定が無効の場合は既存のアラームをキャンセルする。 バックグラウンドスレッドから呼び出し可能。
     *
     * 最適化：前回のスケジュール以降に設定変更がない場合はキャッシュを使用し、 毎回の線形探索（7*24=168時間）を回避する。
     */
    fun schedule(context: Context) {
        val settings = TimeSignalPrefs.load(context)
        if (!settings.enabled) {
            cancel(context)
            ScheduleCache.invalidate(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()
        ) {
            Log.w(TAG, "正確なアラームのパーミッションがありません")
            return
        }

        // キャッシュが有効かを確認。有効なら前回計算した時刻を使用
        val now = System.currentTimeMillis()
        val nextTime =
                if (ScheduleCache.isCacheValid(context, settings)) {
                    val cached = ScheduleCache.getCachedNextAlarmTime()
                    if (cached > now) {
                        Log.d(TAG, "スケジュールキャッシュを使用: ${java.util.Date(cached)}")
                        cached
                    } else {
                        Log.d(TAG, "キャッシュ時刻が過去のため再計算します: ${java.util.Date(cached)}")
                        // 期限切れキャッシュなので次の有効時刻を再計算して保存
                        findNextChimeTime(context, settings)?.also { time ->
                            ScheduleCache.saveNextAlarmTime(context, time, settings)
                        }
                                ?: run {
                                    Log.d(TAG, "有効な次の時報時刻が見つかりませんでした（7日以内に該当なし）")
                                    return
                                }
                    }
                } else {
                    // 無効なら新たに計算して保存
                    findNextChimeTime(context, settings)?.also { time ->
                        ScheduleCache.saveNextAlarmTime(context, time, settings)
                    }
                            ?: run {
                                Log.d(TAG, "有効な次の時報時刻が見つかりませんでした（7日以内に該当なし）")
                                return
                            }
                }

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                makePendingIntent(context),
        )
        Log.d(TAG, "次の時報をスケジュール: ${java.util.Date(nextTime)}")
    }

    /** スケジュール済みのアラームをキャンセルする。 */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(makePendingIntent(context))
        Log.d(TAG, "時報をキャンセルしました")
    }

    /** 現在時刻の次の正時から始めて、設定条件（曜日・時刻・祝日）をすべて満たす 最初の正時を返す。最大7日先まで探索する。 バックグラウンドスレッドから呼び出し可能。 */
    fun findNextChimeTime(context: Context, settings: TimeSignalSettings): Long? {
        // 祝日セットはここで一度だけ読み込む（SharedPreferences の複数読みを避ける）
        val holidayDates =
                if (settings.skipHolidays) {
                    HolidayRepository.getCachedDates(context)
                } else {
                    emptySet()
                }
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val candidate =
                Calendar.getInstance().apply {
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.HOUR_OF_DAY, 1)
                }

        repeat(7 * 24) {
            val hour = candidate.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            val dateStr = dateFmt.format(candidate.time)

            if (hour in settings.startHour..settings.endHour &&
                            dayOfWeek in settings.enabledDays &&
                            !(settings.skipHolidays && dateStr in holidayDates)
            ) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.HOUR_OF_DAY, 1)
        }
        return null
    }

    private fun makePendingIntent(context: Context): PendingIntent {
        val intent =
                Intent(context, TimeSignalReceiver::class.java).apply {
                    action = ACTION_TIME_SIGNAL
                }
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
