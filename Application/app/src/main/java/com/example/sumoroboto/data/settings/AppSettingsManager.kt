package com.example.sumoroboto.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sumo_roboto_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(loadAccentColor())
    val accentColor: StateFlow<AppAccentColor> = _accentColor.asStateFlow()

    private val _robotName = MutableStateFlow(loadRobotName())
    val robotName: StateFlow<String> = _robotName.asStateFlow()

    private val _hasNamedRobot = MutableStateFlow(loadHasNamedRobot())
    val hasNamedRobot: StateFlow<Boolean> = _hasNamedRobot.asStateFlow()

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    private fun loadAccentColor(): AppAccentColor {
        val name = prefs.getString(KEY_ACCENT_COLOR, AppAccentColor.CYAN.name) ?: AppAccentColor.CYAN.name
        return try {
            AppAccentColor.valueOf(name)
        } catch (_: Exception) {
            AppAccentColor.CYAN
        }
    }

    private fun loadRobotName(): String {
        return prefs.getString(KEY_ROBOT_NAME, "Mini Sumo") ?: "Mini Sumo"
    }

    private fun loadHasNamedRobot(): Boolean {
        return prefs.getBoolean(KEY_HAS_NAMED_ROBOT, false)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAccentColor(color: AppAccentColor) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color.name).apply()
        _accentColor.value = color
    }

    fun setRobotName(name: String) {
        val cleanName = name.trim().ifEmpty { "Mini Sumo" }
        prefs.edit()
            .putString(KEY_ROBOT_NAME, cleanName)
            .putBoolean(KEY_HAS_NAMED_ROBOT, true)
            .apply()
        _robotName.value = cleanName
        _hasNamedRobot.value = true
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_ACCENT_COLOR = "pref_accent_color"
        private const val KEY_ROBOT_NAME = "pref_robot_name"
        private const val KEY_HAS_NAMED_ROBOT = "pref_has_named_robot"
    }
}
