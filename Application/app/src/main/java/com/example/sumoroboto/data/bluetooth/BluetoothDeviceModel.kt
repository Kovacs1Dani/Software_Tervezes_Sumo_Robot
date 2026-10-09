package com.example.sumoroboto.data.bluetooth

data class BluetoothDeviceModel(
    val name: String,
    val address: String,
    val isBonded: Boolean = true,
    val isVirtual: Boolean = false
)
