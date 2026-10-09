package com.example.sumoroboto.data.logging

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.example.sumoroboto.data.model.TelemetryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LoggingSessionState(
    val isRecording: Boolean = false,
    val sampleCount: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val latestFile: File? = null
)

class TelemetryCsvLogger(private val context: Context) {

    private val _sessionState = MutableStateFlow(LoggingSessionState())
    val sessionState: StateFlow<LoggingSessionState> = _sessionState.asStateFlow()

    private var currentWriter: BufferedWriter? = null
    private var currentFile: File? = null
    private var sessionStartTime: Long = 0L
    private var sampleCounter: Long = 0L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Starts a new CSV logging session.
     */
    @Synchronized
    fun startLogging(): Boolean {
        if (_sessionState.value.isRecording) return true

        return try {
            val logsDir = File(context.getExternalFilesDir(null), "telemetry_logs").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = fileNameDateFormat.format(Date())
            val file = File(logsDir, "sumo_telemetry_$timestamp.csv")

            val writer = BufferedWriter(FileWriter(file, true))
            // RFC-4180 standard CSV header
            writer.write("Timestamp_ms,DateTime,Dist_Left_cm,Dist_Right_cm,Line_Left,Line_Right,Battery_Volts,Battery_Percent\r\n")
            writer.flush()

            currentFile = file
            currentWriter = writer
            sessionStartTime = System.currentTimeMillis()
            sampleCounter = 0L

            _sessionState.value = LoggingSessionState(
                isRecording = true,
                sampleCount = 0L,
                elapsedSeconds = 0L,
                latestFile = file
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Appends a telemetry sample to the active CSV file.
     */
    @Synchronized
    fun logSample(telemetry: TelemetryData) {
        val writer = currentWriter ?: return
        if (!_sessionState.value.isRecording) return

        try {
            val dateTimeStr = dateFormat.format(Date(telemetry.timestamp))
            val lineLeft = if (telemetry.lineL) 1 else 0
            val lineRight = if (telemetry.lineR) 1 else 0

            val row = buildString {
                append(telemetry.timestamp).append(",")
                append("\"").append(dateTimeStr).append("\",")
                append(telemetry.distL).append(",")
                append(telemetry.distR).append(",")
                append(lineLeft).append(",")
                append(lineRight).append(",")
                append(String.format(Locale.US, "%.2f", telemetry.vBat)).append(",")
                append(telemetry.battery.percentage)
                append("\r\n")
            }

            writer.write(row)
            sampleCounter++

            val elapsedSec = (System.currentTimeMillis() - sessionStartTime) / 1000

            // Periodic flush every 20 samples to avoid data loss
            if (sampleCounter % 20L == 0L) {
                writer.flush()
            }

            _sessionState.value = _sessionState.value.copy(
                sampleCount = sampleCounter,
                elapsedSeconds = elapsedSec
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stops the logging session and flushes any buffered data.
     */
    @Synchronized
    fun stopLogging(): File? {
        val file = currentFile
        try {
            currentWriter?.flush()
            currentWriter?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentWriter = null
        }

        _sessionState.value = _sessionState.value.copy(
            isRecording = false,
            latestFile = file
        )
        return file
    }

    /**
     * Opens the Android share sheet to export the latest recorded CSV file.
     */
    fun shareLatestCsv(): Boolean {
        val file = _sessionState.value.latestFile ?: currentFile ?: return false
        if (!file.exists() || file.length() == 0L) return false

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = ShareCompat.IntentBuilder(context)
                .setType("text/csv")
                .setSubject("Mini Sumo Robot Telemetry Log")
                .setStream(uri)
                .setChooserTitle("Export Telemetry CSV")
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(shareIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Lists existing recorded CSV log files.
     */
    fun getSavedLogFiles(): List<File> {
        val logsDir = File(context.getExternalFilesDir(null), "telemetry_logs")
        if (!logsDir.exists()) return emptyList()
        return logsDir.listFiles { f -> f.extension.equals("csv", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
