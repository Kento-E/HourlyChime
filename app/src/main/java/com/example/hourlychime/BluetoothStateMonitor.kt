package com.example.hourlychime

/**
 * Bluetooth接続状態をメモリキャッシュで管理する。 BluetoothEventReceiver から更新され、TimeSignalReceiver から参照される。
 * これにより、毎アラーム受信時の Bluetooth 接続確認スキャンを削減する。
 */
object BluetoothStateMonitor {
  @Volatile private var cachedConnectedAddresses: Set<String> = emptySet()

  /**
   * キャッシュが少なくとも一度更新されたかどうかを示すフラグ。
   * プロセス再起動直後はfalseで、updateConnectedDevices()の初回呼び出し後にtrueになる。
   */
  @Volatile private var initialized: Boolean = false

  /** Bluetooth接続イベント受信時に呼び出される。新しい接続アドレスセットを更新する。 */
  fun updateConnectedDevices(addresses: Set<String>) {
    cachedConnectedAddresses = addresses.toSet()
    initialized = true
  }

  /** キャッシュされた接続状態から対象デバイスのいずれかが接続中かを判定する。 */
  fun isAnyTargetDeviceConnectedFromCache(targetAddresses: Set<String>): Boolean {
    if (targetAddresses.isEmpty()) return false
    return targetAddresses.any { it in cachedConnectedAddresses }
  }

  /** キャッシュされた接続中アドレス一覧を返す。 */
  fun getCachedConnectedAddresses(): Set<String> = cachedConnectedAddresses.toSet()

  /**
   * キャッシュが初期化済みかどうかを返す。
   * プロセス再起動後はfalseとなるため、未初期化時は直接スキャンへフォールバックすることを推奨する。
   */
  fun isInitialized(): Boolean = initialized

  /** テスト専用：キャッシュと初期化状態をリセットする。 */
  internal fun resetForTest() {
    cachedConnectedAddresses = emptySet()
    initialized = false
  }
}
