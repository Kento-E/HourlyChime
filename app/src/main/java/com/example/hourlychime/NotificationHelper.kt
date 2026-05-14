package com.example.hourlychime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "time_signal_channel_v3"
    private const val NOTIFICATION_ID = 2001
    private val FIXED_VIBRATION_PATTERN = longArrayOf(0L, 120L, 100L, 120L)

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
                            // バイブはチャネル側ではなくアプリ側で固定制御する。
                            enableVibration(false)
                        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    /** 時報通知を発行する。 */
    fun postTimeSignal(context: Context, hour: Int) {
        vibrateFixedPattern(context)

        val text = "%02d:00 の時報".format(hour)
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setLargeIcon(largeIcon)
                .setContentTitle("時報")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000L) // 1分後に自動削除
                .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun vibrateFixedPattern(context: Context) {
        val vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VibratorManager::class.java)
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(FIXED_VIBRATION_PATTERN, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(FIXED_VIBRATION_PATTERN, -1)
        }
    }
}
