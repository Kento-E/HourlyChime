package com.example.hourlychime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothHelper {

    /** BLUETOOTH_CONNECT 権限が付与されているかを返す（API 31+ の場合のみ必要）。 */
    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * ペアリング済みデバイスの一覧を返す。
     * 権限がない場合は空リストを返す。
     */
    @SuppressLint("MissingPermission")
    fun getBondedDevices(context: Context): List<BluetoothDevice> {
        if (!hasBluetoothConnectPermission(context)) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return emptyList()
        return manager.adapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * 対象アドレスのいずれかのデバイスが現在接続中かどうかを返す。
     * 権限がない場合は false を返す（フェイルクローズ）。
     */
    fun isAnyTargetDeviceConnected(context: Context, targetAddresses: Set<String>): Boolean {
        if (targetAddresses.isEmpty()) return false
        if (!hasBluetoothConnectPermission(context)) return false
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return false
        val adapter = manager.adapter ?: return false
        return targetAddresses.any { address ->
            try {
                val device = adapter.getRemoteDevice(address)
                isDeviceConnected(device)
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * BluetoothDevice が現在接続中かどうかを返す。
     * `isConnected()` は隠しAPIだが全APIレベルで安定して存在する。
     */
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as Boolean
        } catch (e: Exception) {
            false
        }
    }
}
