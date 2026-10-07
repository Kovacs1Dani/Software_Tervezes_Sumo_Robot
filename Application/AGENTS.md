# AGENTS.md - Mini Sumo Telemetry & Remote Control System

This document serves as the single source of truth for AI agents and developers working on the Mini Sumo Robot control, telemetry, and logging platform.

---

## 1. Project Overview & Architecture
The system consists of three coordinated components communicating via Bluetooth:
1. **Firmware (`/firmware`)**: Arduino Nano (ATmega328P) controlling DC motors, reading distance/line sensors, and measuring battery voltage.
2. **Android Application (`/android`)**: Native Android client (Kotlin + Jetpack Compose) for low-latency manual control, live telemetry display, CSV logging, and Dohyo visualization.
3. **Web Interface (`/web`)**: Lightweight Web Bluetooth API interface (HTML/JS + Chart.js) for quick cross-platform sensor debugging and CSV export.

Your main focus should only be creating the Kotlin + Jetpack Compose application which cnstist of the following elements:

A low-latency, real-time native Android app designed to control an autonomous/RC Mini Sumo robot, display live telemetry, log sensor data to CSV, and visualize Dohyo ring tracking.

- **Platform:** Android (Min SDK 26, Target SDK 34+)
- **Language:** Kotlin
- **UI Toolkit:** 100% Jetpack Compose (Material 3)
- **Architecture:** MVVM with Unidirectional Data Flow (UDF)
- **Concurrency:** Kotlin Coroutines (`Dispatchers.IO` for BT I/O, `Dispatchers.Main` for UI) + `StateFlow` / `SharedFlow`
- **Bluetooth Stack:** Android Bluetooth Classic (RFCOMM / SPP)
  - Standard SPP UUID: `00001101-0000-1000-8000-00805F9B34FB`
  - Target module: HC-05 / HC-06

Core Features & Screen Structure

The UI utilizes a persistent **TopAppBar**, a **BottomNavigationBar**, and three main screen destinations:

1.
    Connection Status (Top Bar / Initial Dialog):

    If there is no connection, a pop-up card lists the paired Bluetooth devices, and you can connect from there.

    Once a connection is established, the connection status and the mini battery icon showing the charge level are continuously displayed in the header.

2.
     Menu Item – Manual Remote Control:

    Virtual D-Pad or touchscreen joystick.

    Speed limit slider (0–100%).

    Mode Switch: MANUAL (you control it) vs. AUTONOMOUS (the robot runs its own sumo search-and-attack algorithm).

    Emergency Stop button.
3. 
     Menu Item – Telemetry & Logging:

    Distance sensors (e.g., left/right distance in cm or raw voltage).

    Line sensor status (black/white edge).

    Data logging panel: “Start Logging” button, timestamp counter, and “Export CSV” button to save measurements to your phone

4. 
    Menu Item3 – Map-Based Tracking (Nice-to-have):

    Since there is no GPS indoors, you draw the standard Dohyo arena (a 77-cm black circle with a 2.5-cm white border) on a Compose Canvas.

### 1. Persistent TopAppBar
- **Connection Status:** Indicator dot (Red = Disconnected, Amber = Connecting, Green = Connected) and target device name.
- **Connect / Disconnect Action:** Clicking opens a modal sheet listing system-bonded devices (`BluetoothAdapter.bondedDevices`).
- **Live Battery Widget:** Calculates percentage from incoming `vBat`.
  - 2S LiPo mapping: `8.4V` = 100%, `7.4V` = ~50%, `< 6.8V` = Critical warning state (red alert).

### 2. Screen 1: Remote Control (`ControlScreen`)
- **Virtual Joystick / D-Pad:** 2-axis input mapped to differential steering (`pwmL`, `pwmR` range: `-255..255`).
- **Speed Limiter Slider:** Scales max PWM output (0% to 100%).
- **Mode Toggle:** Button/Switch to toggle between `MANUAL` and `AUTO` sumo fight mode.
- **Emergency Stop Button:** Prominent red button that immediately sends `STOP\n` and zeroes out inputs.

### 3. Screen 2: Telemetry & Logging (`TelemetryScreen`)
- **Live Sensor Cards:**
  - Front/Side Distance Sensors: Visual bars and integer distance values.
  - Border/Line Sensors: Binary indicators (Clear vs. White Dohyo Border detected).
- **Session Logger Panel:**
  - `Start Logging` / `Stop Logging` toggle with timer and sample count.
  - Generates RFC-4180 compliant CSV files saved in `context.getExternalFilesDir()`.
  - `Export / Share CSV` button invoking Android `ShareCompat` / `Intent.ACTION_SEND` via `FileProvider`.

### 4. Screen 3: Dohyo Map & Tracking (`DohyoScreen`)
- **Canvas Rendering:** Circular Dohyo ring (standard ratio: 77 cm dia[118;1:3umeter black ring with 2.5 cm white border).
- **Radar / Sensor Visualization:** Draws distance sensor projection cones and highlights border collision zones based on active telemetry.

---

## 3. Communication Protocol (Hardware UART @ 115200 bps)

All communications are line-delimited with `\n` (ASCII 10).

### Outgoing: Phone -> Robot (Rate-Limited to 30–50 Hz)
| Command Packet | Arguments | Description |
|---|---|---|
| `M,<pwmL>,<pwmR>\n` | `Int` (-255 to 255) | Differential motor speeds |
| `MODE,<STATE>\n` | `MANUAL` or `AUTO` | Switches operational state |
| `STOP\n` | None | Immediate motor shutoff |

### Incoming: Robot -> Phone (Telemetry Stream)
| Packet Format | Field Breakdown |
|---|---|
| `TEL,<distL>,<distR>,<lineL>,<lineR>,<vBat>\n` | - `distL`, `distR` (Int): Distance readings<br>- `lineL`, `lineR` (Int: `0` or `1`): Border line detected<br>- `vBat` (Float): Battery voltage (e.g. `7.84`) |

---

## 4. Critical Engineering Rules for Agents

### 1. Bluetooth Buffer Framing & Deserialization
- **NEVER assume `socket.inputStream.read()` returns complete lines.** Bluetooth packets arrive fragmented.
- Always accumulate incoming bytes into a persistent thread-safe buffer or use a `BufferedReader.readLine()` loop inside a dedicated Coroutine running on `Dispatchers.IO`.
- Discard malformed packets gracefully without crashing the parser.

### 2. Rate-Limiting Outgoing Packets (No Flooding)
- **DO NOT emit Bluetooth write calls directly on touch drag events.**
- Touch gestures must ONLY update a local UI state (`targetPwmL`, `targetPwmR`).
- A dedicated Coroutine ticker loop MUST sample the latest values and send `M,<L>,<R>\n` packets at **30–50 Hz (every 20–33 ms)**.

### 3. Permissions & Lifecycle
- Support Android 12+ preferably 15/16 (API 31+) runtime permissions:
  - `android.permission.BLUETOOTH_SCAN`
  - `android.permission.BLUETOOTH_CONNECT`
- Automatically close sockets and release resources when the ViewModel is cleared or the connection drops.
- Implement an explicit reconnect state machine.

---

## 2. Communication Protocol (Hardware UART)

- **Baud Rate:** 115200 baud (or 57600 baud). Hardware UART only (`Serial`, pins D0/D1). `SoftwareSerial` is strictly prohibited.
- **Line Delimiter:** Every packet MUST terminate with `\n` (ASCII 10).
- **Frequency:** Transmitted at fixed 30–50 Hz (every 20–33 ms). No flooding on touchscreen touch events.


The hardware configuration is not fixated yet you shoul only build a swappable universal framework if you want to implement it but the parts are going to be modified.
### Phone/Web -> Robot (Commands)
| Command Packet | Description | Example |
|---|---|---|
| `M,<pwmL>,<pwmR>\n` | Drive motors (-255 to 255) | `M,150,150\n` (forward), `M,-100,100\n` (spin) |
| `MODE,<STATE>\n` | Switch between `MANUAL` and `AUTO` | `MODE,MANUAL\n` |
| `STOP\n` | Immediate emergency motor cut-off | `STOP\n` |


---

## 3. Hardware & Power Constraints

- **Power Source:** 2S LiPo battery (Nominal 7.4V, Full 8.4V).
- **Power Rail Isolation:** 
  - Battery goes directly to motor driver (e.g., TB6612FNG) with a 470µF–1000µF electrolytic capacitor across `VM` and `GND`.
  - Logic (Arduino Nano + Bluetooth) is powered by a dedicated 5V Buck Step-Down converter (Mini360).
- **Battery Measurement (`A0`):** Simple 2-resistor voltage divider (10kΩ + 10kΩ) divides max 8.4V down to max 4.2V. Formula: `vBat = analogRead(A0) * (5.0 / 1023.0) * 2.0`.
- **Bluetooth Modules:**
  - Android native: HC-05 (Bluetooth Classic / SPP profile, UUID: `00001101-0000-1000-8000-00805F9B34FB`).
  - Web interface: HM-10 / AT-09 (Bluetooth Low Energy / GATT UART service).
- **Safety Watchdog:** If no valid mot:or command packet is received within 200 ms, the Arduino MUST automatically cut motor power (`stopMotors()`).

---

## 4. Software Implementation Guidelines

### Firmware (`/firmware`)
- **Non-blocking loops only:** The `delay()` function is banned in the main loop. Use `millis()` delta timers for telemetry transmission.
- **Serial Buffering:** Non-blocking `Serial.read()` assembling characters into a fixed-size buffer until `\n` is encountered.

### Android Client (`/android`)
- **UI Framework:** 100% Jetpack Compose (no XML views).
- **State Management:** Unidirectional Data Flow (UDF) with `ViewModel` and Kotlin `StateFlow`.
- **Concurrency:** Kotlin Coroutines (`Dispatchers.IO`) for socket reads/writes.
- **Bluetooth Streaming:** Use a 50 Hz loop ticker for sending `M,<L>,<R>\n` packets based on joystick state.
- **Data Logging:** Buffer incoming `TEL` rows and flush to a local `.csv` file in app storage with share intent support.
- **Permissions:** Handle Android 12+ runtime permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`).

### Web Client (`/web`)
- Single-page application using vanilla modern JavaScript (ES6+).
- Use `navigator.bluetooth.requestDevice` with the HM-10 UART Service UUID (`0[118;1:3u000ffe0-0000-1000-8000-00805f9b34fb`).
- Live plotting using Chart.js with ring-buffered sample queues.



