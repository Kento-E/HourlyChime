package com.example.hourlychime

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Bluetooth接続/切断イベントを受信してキャッシュを更新する。 AndroidManifest.xml に登録し、アプリ起動後は常時バックグラウンドで動作。
 * これにより、TimeSignalReceiver での接続確認がスキャン不要になる。
 */
class BluetoothEventReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "BluetoothEventReceiver"
  }

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      BluetoothAdapter.ACTION_STATE_CHANGED -> {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        if (state == BluetoothAdapter.STATE_OFF) {
          BluetoothStateMonitor.updateConnectedDevices(emptySet())
          Log.d(TAG, "Bluetooth OFF: キャッシュをクリア")
        }
      }
      BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
        // 接続状態の変化を検知してキャッシュを再構築
        if (BluetoothHelper.hasBluetoothConnectPermission(context)) {
          val connectedAddresses = queryConnectedDeviceAddresses(context)
          BluetoothStateMonitor.updateConnectedDevices(connectedAddresses)
        }
      }
    }
  }

  /** 現在接続中のすべてのBluetoothデバイスのMACアドレスを取得する。 */
  private fun queryConnectedDeviceAddresses(context: Context): Set<String> {
    return try {
      BluetoothHelper.getBondedDevices(context)
              .filter { device -> BluetoothHelper.isDeviceConnectedPublic(device) }
              .mapTo(mutableSetOf()) { it.address }
    } catch (e: Exception) {
      Log.w(TAG, "接続中デバイス情報取得失敗", e)
      emptySet()
    }
  }
}
