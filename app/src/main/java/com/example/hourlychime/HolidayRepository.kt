package com.example.hourlychime

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.json.JSONObject

/**
 * 日本の国民の祝日データを管理するリポジトリ。
 *
 * データソース: https://holidays-jp.github.io/api/v1/date.json （内閣府が公表する祝日をもとにしたコミュニティAPIミラー）
 *
 * 取得データは SharedPreferences にキャッシュし、7日間有効とする。 ネットワーク通信は refreshSync() でのみ行い、isHoliday()
 * はキャッシュのみ参照する。
 */
object HolidayRepository {
    private const val TAG = "HolidayRepository"
    private const val PREF_NAME = "holiday_cache"
    private const val KEY_DATES = "holiday_dates"
    private const val KEY_CACHED_AT = "cached_at"
    private const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30日（7日から拡張、ネットワーク通信削減）
    private const val API_URL = "https://holidays-jp.github.io/api/v1/date.json"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** キャッシュ済みの祝日日付セットを返す（ネットワーク通信なし）。 */
    fun getCachedDates(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_DATES, null)
        return if (!cached.isNullOrEmpty()) cached.split(",").toSet() else emptySet()
    }

    /** 指定した日付が祝日かどうかをキャッシュから判定する。 */
    fun isHoliday(context: Context, calendar: Calendar): Boolean {
        val dateStr = dateFormat.format(calendar.time)
        return getCachedDates(context).contains(dateStr)
    }

    /** キャッシュの有効期限が切れているかどうかを返す。 */
    fun isCacheExpired(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cachedAt = prefs.getLong(KEY_CACHED_AT, 0L)
        return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS
    }

    /**
     * ネットワークから祝日データを取得してキャッシュを更新する。 バックグラウンドスレッドで呼び出すこと。
     * @return 成功した場合 true
     */
    fun refreshSync(context: Context): Boolean {
        return try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "GET"
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            conn.disconnect()

            val json = JSONObject(body)
            val dates = buildSet<String> { json.keys().forEach { add(it) } }

            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_DATES, dates.joinToString(","))
                    .putLong(KEY_CACHED_AT, System.currentTimeMillis())
                    .apply()

            Log.d(TAG, "祝日データを更新しました (${dates.size}件)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "祝日データ取得失敗", e)
            false
        }
    }
}
