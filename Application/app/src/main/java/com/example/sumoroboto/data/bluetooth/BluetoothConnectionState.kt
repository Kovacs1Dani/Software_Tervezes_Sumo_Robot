package com.example.sumoroboto.data.bluetooth

sealed interface BluetoothConnectionState {
    object Disconnected : BluetoothConnectionState
    data class Connecting(val deviceName: String) : BluetoothConnectionState
    data class Connected(val deviceName: String, val address: String) : BluetoothConnectionState
    data class Error(val message: String) : BluetoothConnectionState
}
