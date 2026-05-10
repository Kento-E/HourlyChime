package com.example.hourlychime

import android.content.Context
import android.util.Log

/**
 * 次のアラーム時刻をメモリ＆SharedPreferences にキャッシュして保存する。 設定変更時のみ再計算し、毎アラーム受信時の線形探索（7*24=168時間）を削減。 端末再起動時も
 * SharedPreferences から復元される。
 */
object ScheduleCache {
  private const val TAG = "ScheduleCache"
  private const val PREF_NAME = "schedule_cache"
  private const val KEY_NEXT_ALARM_TIME = "next_alarm_time_ms"
  private const val KEY_CACHED_SETTINGS_HASH = "settings_hash"

  @Volatile private var cachedNextAlarmTime: Long = -1L

  /** キャッシュされた次のアラーム時刻を返す。-1L の場合は無効（再計算が必要）。 */
  fun getCachedNextAlarmTime(): Long = cachedNextAlarmTime

  /**
   * キャッシュが有効かどうかを判定する。
   * - メモリキャッシュが存在する場合は有効
   * - 設定ハッシュが変わっている場合は無効（設定変更を検知）
   */
  fun isCacheValid(context: Context, settings: TimeSignalSettings): Boolean {
    if (cachedNextAlarmTime <= 0L) {
      Log.d(TAG, "メモリキャッシュなし")
      return false
    }

    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val oldHash = prefs.getInt(KEY_CACHED_SETTINGS_HASH, 0)
    val newHash = settings.hashCode()
    if (oldHash != newHash) {
      Log.d(TAG, "設定が変更されています (hash: $oldHash -> $newHash)")
      return false
    }

    return true
  }

  /** 次のアラーム時刻をキャッシュに保存する。 */
  fun saveNextAlarmTime(context: Context, timeMs: Long, settings: TimeSignalSettings) {
    cachedNextAlarmTime = timeMs
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_NEXT_ALARM_TIME, timeMs)
            .putInt(KEY_CACHED_SETTINGS_HASH, settings.hashCode())
            .apply()
    Log.d(TAG, "次のアラーム時刻をキャッシュ: ${java.util.Date(timeMs)}")
  }

  /** SharedPreferences からキャッシュを復元する。 */
  fun restoreFromPrefs(context: Context) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    cachedNextAlarmTime = prefs.getLong(KEY_NEXT_ALARM_TIME, -1L)
    Log.d(
            TAG,
            "キャッシュ復元: ${if (cachedNextAlarmTime > 0) java.util.Date(cachedNextAlarmTime) else "無効"}"
    )
  }

  /** キャッシュをクリアする。設定変更時に明示的に呼び出す。 */
  fun invalidate(context: Context) {
    cachedNextAlarmTime = -1L
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_NEXT_ALARM_TIME)
            .remove(KEY_CACHED_SETTINGS_HASH)
            .apply()
    Log.d(TAG, "スケジュールキャッシュを無効化")
  }
}
