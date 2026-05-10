package com.example.hourlychime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "time_signal_channel_v2"
    private const val NOTIFICATION_ID = 2001
    private val DEFAULT_VIBRATION_PATTERN = longArrayOf(0L, 120L, 100L, 120L)

    /** 先頭は待機時間(ms)。例: 0, 120, 100, 120 = 短いバイブ2連。 */
    @Volatile private var vibrationPattern: LongArray = DEFAULT_VIBRATION_PATTERN.copyOf()

    fun setVibrationPattern(pattern: LongArray) {
        require(pattern.isNotEmpty()) { "Vibration pattern must not be empty." }
        vibrationPattern = pattern.copyOf()
    }

    /** 時報通知チャンネルを作成する。アプリ起動時に必ず呼び出すこと（冪等）。 */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs =
                AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

        val channel =
                NotificationChannel(
                                CHANNEL_ID,
                                "時報",
                                NotificationManager.IMPORTANCE_HIGH,
                        )
                        .apply {
                            description = "毎正時の時報通知"
                            setSound(soundUri, audioAttrs)
                            enableVibration(true)
                            setVibrationPattern(vibrationPattern)
                        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    /** 時報通知を発行する。 */
    fun postTimeSignal(context: Context, hour: Int) {
        val text = "%02d:00 の時報".format(hour)
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("時報")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(vibrationPattern)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000L) // 1分後に自動削除
                .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}
