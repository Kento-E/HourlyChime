package com.example.hourlychime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BluetoothStateMonitorTest {

    @Before
    fun setUp() {
        // 各テスト前にキャッシュをリセットする
        BluetoothStateMonitor.resetForTest()
    }

    @Test
    fun `初期状態ではinitializedがfalseである`() {
        assertFalse(BluetoothStateMonitor.isInitialized())
    }

    @Test
    fun `updateConnectedDevices呼び出し後はinitializedがtrueになる`() {
        BluetoothStateMonitor.updateConnectedDevices(emptySet())
        assertTrue(BluetoothStateMonitor.isInitialized())
    }

    @Test
    fun `空セットで更新してもinitializedはtrueになる`() {
        BluetoothStateMonitor.updateConnectedDevices(emptySet())
        assertTrue(BluetoothStateMonitor.isInitialized())
        assertFalse(BluetoothStateMonitor.isAnyTargetDeviceConnectedFromCache(setOf("AA:BB:CC:DD:EE:FF")))
    }

    @Test
    fun `対象デバイスが接続中の場合はtrueを返す`() {
        val address = "AA:BB:CC:DD:EE:FF"
        BluetoothStateMonitor.updateConnectedDevices(setOf(address))
        assertTrue(BluetoothStateMonitor.isAnyTargetDeviceConnectedFromCache(setOf(address)))
    }

    @Test
    fun `対象デバイスが未接続の場合はfalseを返す`() {
        BluetoothStateMonitor.updateConnectedDevices(setOf("11:22:33:44:55:66"))
        assertFalse(BluetoothStateMonitor.isAnyTargetDeviceConnectedFromCache(setOf("AA:BB:CC:DD:EE:FF")))
    }

    @Test
    fun `対象アドレスが空の場合はfalseを返す`() {
        BluetoothStateMonitor.updateConnectedDevices(setOf("AA:BB:CC:DD:EE:FF"))
        assertFalse(BluetoothStateMonitor.isAnyTargetDeviceConnectedFromCache(emptySet()))
    }

    @Test
    fun `複数の対象デバイスのいずれかが接続中ならtrueを返す`() {
        BluetoothStateMonitor.updateConnectedDevices(setOf("11:22:33:44:55:66"))
        assertTrue(
            BluetoothStateMonitor.isAnyTargetDeviceConnectedFromCache(
                setOf("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66")
            )
        )
    }

    @Test
    fun `getCachedConnectedAddressesは現在のキャッシュを返す`() {
        val addresses = setOf("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66")
        BluetoothStateMonitor.updateConnectedDevices(addresses)
        assertEquals(addresses, BluetoothStateMonitor.getCachedConnectedAddresses())
    }
}
