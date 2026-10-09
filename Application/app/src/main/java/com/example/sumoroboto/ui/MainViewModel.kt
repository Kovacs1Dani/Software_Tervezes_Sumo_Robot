package com.example.sumoroboto.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sumoroboto.data.bluetooth.BluetoothConnectionState
import com.example.sumoroboto.data.bluetooth.BluetoothDeviceModel
import com.example.sumoroboto.data.bluetooth.BluetoothManager
import com.example.sumoroboto.data.logging.LoggingSessionState
import com.example.sumoroboto.data.logging.TelemetryCsvLogger
import com.example.sumoroboto.data.model.BatteryState
import com.example.sumoroboto.data.model.RobotMode
import com.example.sumoroboto.data.model.TelemetryData
import com.example.sumoroboto.ui.navigation.ScreenDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothManager = BluetoothManager(application.applicationContext, viewModelScope)
    val csvLogger = TelemetryCsvLogger(application.applicationContext)

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState

    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val _currentMode = MutableStateFlow(RobotMode.MANUAL)
    val currentMode: StateFlow<RobotMode> = _currentMode.asStateFlow()

    private val _speedLimit = MutableStateFlow(0.80f) // 80% default
    val speedLimit: StateFlow<Float> = _speedLimit.asStateFlow()

    private val _joystickX = MutableStateFlow(0f)
    val joystickX: StateFlow<Float> = _joystickX.asStateFlow()

    private val _joystickY = MutableStateFlow(0f)
    val joystickY: StateFlow<Float> = _joystickY.asStateFlow()

    private val _currentPwmL = MutableStateFlow(0)
    val currentPwmL: StateFlow<Int> = _currentPwmL.asStateFlow()

    private val _currentPwmR = MutableStateFlow(0)
    val currentPwmR: StateFlow<Int> = _currentPwmR.asStateFlow()

    private val _currentDestination = MutableStateFlow(ScreenDestination.CONTROL)
    val currentDestination: StateFlow<ScreenDestination> = _currentDestination.asStateFlow()

    private val _showDeviceDialog = MutableStateFlow(false)
    val showDeviceDialog: StateFlow<Boolean> = _showDeviceDialog.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDeviceModel>> = _bondedDevices.asStateFlow()

    val loggingSession: StateFlow<LoggingSessionState> = csvLogger.sessionState

    init {
        // Collect telemetry stream
        viewModelScope.launch {
            bluetoothManager.telemetryFlow.collect { data ->
                _telemetry.value = data
                _batteryState.value = data.battery
                csvLogger.logSample(data)
            }
        }

        refreshDevices()
    }

    fun refreshDevices() {
        _bondedDevices.value = bluetoothManager.getBondedDevices()
    }

    fun openDeviceDialog() {
        refreshDevices()
        _showDeviceDialog.value = true
    }

    fun closeDeviceDialog() {
        _showDeviceDialog.value = false
    }

    fun connectDevice(device: BluetoothDeviceModel) {
        closeDeviceDialog()
        bluetoothManager.connect(device)
    }

    fun disconnectDevice() {
        bluetoothManager.disconnect()
    }

    fun setDestination(destination: ScreenDestination) {
        _currentDestination.value = destination
    }

    fun setSpeedLimit(limit: Float) {
        _speedLimit.value = limit.coerceIn(0.1f, 1.0f)
        recomputeMotorPwm(_joystickX.value, _joystickY.value)
    }

    fun setRobotMode(mode: RobotMode) {
        _currentMode.value = mode
        bluetoothManager.setMode(mode)
    }

    fun toggleMode() {
        val next = if (_currentMode.value == RobotMode.MANUAL) RobotMode.AUTO else RobotMode.MANUAL
        setRobotMode(next)
    }

    /**
     * Called by Joystick component on drag.
     * x: -1.0 (left) to 1.0 (right)
     * y: -1.0 (reverse) to 1.0 (forward)
     */
    fun onJoystickMove(x: Float, y: Float) {
        _joystickX.value = x
        _joystickY.value = y
        recomputeMotorPwm(x, y)
    }

    fun onJoystickRelease() {
        _joystickX.value = 0f
        _joystickY.value = 0f
        _currentPwmL.value = 0
        _currentPwmR.value = 0
        bluetoothManager.updateTargetMotorSpeed(0, 0)
    }

    private fun recomputeMotorPwm(x: Float, y: Float) {
        val maxPwm = (255 * _speedLimit.value).roundToInt()

        // Differential steering calculation:
        // y > 0 is forward, y < 0 is reverse
        // x > 0 is right turn (left wheel faster, right wheel slower)
        // x < 0 is left turn (right wheel faster, left wheel slower)
        val left = ((y + x) * maxPwm).roundToInt().coerceIn(-255, 255)
        val right = ((y - x) * maxPwm).roundToInt().coerceIn(-255, 255)

        _currentPwmL.value = left
        _currentPwmR.value = right

        // Update target in BluetoothManager without flooding socket;
        // ticker loop will sample this at 40 Hz
        bluetoothManager.updateTargetMotorSpeed(left, right)
    }

    fun emergencyStop() {
        _joystickX.value = 0f
        _joystickY.value = 0f
        _currentPwmL.value = 0
        _currentPwmR.value = 0
        bluetoothManager.emergencyStop()
    }

    fun toggleLogging() {
        if (loggingSession.value.isRecording) {
            csvLogger.stopLogging()
        } else {
            csvLogger.startLogging()
        }
    }

    fun shareCsv() {
        csvLogger.shareLatestCsv()
    }

    override fun onCleared() {
        super.onCleared()
        csvLogger.stopLogging()
        bluetoothManager.disconnect()
    }
}
