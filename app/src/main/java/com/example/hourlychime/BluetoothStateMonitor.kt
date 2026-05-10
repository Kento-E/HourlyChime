package com.example.hourlychime

import android.util.Log

/**
 * Bluetooth接続状態をメモリキャッシュで管理する。 BluetoothEventReceiver から更新され、TimeSignalReceiver から参照される。
 * これにより、毎アラーム受信時の Bluetooth 接続確認スキャンを削減する。
 */
object BluetoothStateMonitor {
  private const val TAG = "BluetoothStateMonitor"
  @Volatile private var cachedConnectedAddresses: Set<String> = emptySet()

  /** Bluetooth接続イベント受信時に呼び出される。新しい接続アドレスセットを更新する。 */
  fun updateConnectedDevices(addresses: Set<String>) {
    cachedConnectedAddresses = addresses.toSet()
    Log.d(TAG, "接続中のBluetoothデバイスを更新: $addresses")
  }

  /** キャッシュされた接続状態から対象デバイスのいずれかが接続中かを判定する。 */
  fun isAnyTargetDeviceConnectedFromCache(targetAddresses: Set<String>): Boolean {
    if (targetAddresses.isEmpty()) return false
    return targetAddresses.any { it in cachedConnectedAddresses }
  }

  /** キャッシュされた接続中アドレス一覧を返す。 */
  fun getCachedConnectedAddresses(): Set<String> = cachedConnectedAddresses.toSet()
}
