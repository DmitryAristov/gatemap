package com.daristov.checkpoint.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.R
import com.daristov.checkpoint.screens.mapscreen.MapInitStep.DONE
import com.daristov.checkpoint.screens.mapscreen.MapInitStep.LOADING_CUSTOMS
import com.daristov.checkpoint.screens.mapscreen.MapInitStep.LOADING_LOCATION
import com.daristov.checkpoint.screens.mapscreen.MapInitStep.LOADING_MAP
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppThemeMode() {
    LIGHT,
    DARK,
    SYSTEM;

    @StringRes
    fun labelRes(): Int = when (this) {
        LIGHT -> R.string.theme_light
        DARK -> R.string.theme_dark
        SYSTEM -> R.string.theme_system
    }
}

enum class AppLanguage(val code: String, val label: String) {
    RU("ru", "Русский"),
    KZ("kk", "Қазақша"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code } ?: RU
    }

    override fun toString(): String = label
}

val DEFAULT_THEME = AppThemeMode.SYSTEM
val DEFAULT_LANGUAGE = AppLanguage.RU
const val DEFAULT_AUTO_DAY_NIGHT_DETECT = true
const val DEFAULT_3D_ENABLED = true
const val DEFAULT_ALLOW_STATS = true
const val DEFAULT_FIRST_LAUNCH = true
const val DEFAULT_STABLE_TRAJECTORY_SENSITIVITY = 50
const val DEFAULT_VERTICAL_MOVEMENT_SENSITIVITY = 50
const val DEFAULT_HORIZONTAL_COMPRESSION_SENSITIVITY = 50

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val pref = SettingsPreferenceManager(application.applicationContext)

    private val _themeMode = MutableStateFlow(DEFAULT_THEME)
    private val _language = MutableStateFlow(DEFAULT_LANGUAGE)
    private val _autoDayNightDetect = MutableStateFlow(DEFAULT_AUTO_DAY_NIGHT_DETECT)
    private val _is3DEnabled = MutableStateFlow(DEFAULT_3D_ENABLED)
    private val _stableTrajectorySensitivity = MutableStateFlow(DEFAULT_STABLE_TRAJECTORY_SENSITIVITY)
    private val _verticalMovementSensitivity = MutableStateFlow(DEFAULT_VERTICAL_MOVEMENT_SENSITIVITY)
    private val _horizontalCompressionSensitivity = MutableStateFlow(DEFAULT_HORIZONTAL_COMPRESSION_SENSITIVITY)
    private val _allowStats = MutableStateFlow(DEFAULT_ALLOW_STATS)
    private val _isFirstLaunch = MutableStateFlow(DEFAULT_FIRST_LAUNCH)

    val themeMode: StateFlow<AppThemeMode> = _themeMode
    val language: StateFlow<AppLanguage> = _language
    val autoDayNightDetect: StateFlow<Boolean> = _autoDayNightDetect
    val is3DEnabled: StateFlow<Boolean> = _is3DEnabled
    val stableTrajectorySensitivity: StateFlow<Int> = _stableTrajectorySensitivity
    val verticalMovementSensitivity: StateFlow<Int> = _verticalMovementSensitivity
    val horizontalCompressionSensitivity: StateFlow<Int> = _horizontalCompressionSensitivity
    var selectedAlarmUri by mutableStateOf<Uri?>(null)
        private set
    val allowStats: StateFlow<Boolean> = _allowStats
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch

    init {
        viewModelScope.launch {
            pref.getTheme().collect { _themeMode.value = it }
        }

        viewModelScope.launch {
            pref.getLanguage().collect { _language.value = it }
        }

        viewModelScope.launch {
            pref.getAutoDayNightDetect().collect { _autoDayNightDetect.value = it }
        }

        viewModelScope.launch {
            pref.is3DEnabled().collect { _is3DEnabled.value = it }
        }

        viewModelScope.launch {
            pref.getStableTrajectoryRatio().collect { _stableTrajectorySensitivity.value = it }
        }

        viewModelScope.launch {
            pref.getVerticalMovementSensitivity().collect { _verticalMovementSensitivity.value = it }
        }

        viewModelScope.launch {
            pref.getHorizontalCompressionSensitivity().collect { _horizontalCompressionSensitivity.value = it }
        }

        viewModelScope.launch {
            pref.getAlarmUri().collect { selectedAlarmUri = it }
        }

        viewModelScope.launch {
            pref.getAllowStats().collect { _allowStats.value = it }
        }

        viewModelScope.launch {
            pref.isFirstLaunch().collect { _isFirstLaunch.value = it }
        }
    }

    fun changeTheme(mode: AppThemeMode) {
        viewModelScope.launch {
            pref.setTheme(mode)
        }
    }

    fun changeLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            pref.setLanguage(lang)
        }
    }

    fun changeAutoDayNightDetect(value: Boolean) {
        _autoDayNightDetect.value = value
        viewModelScope.launch {
            pref.setAutoDayNightDetect(value)
        }
    }

    fun change3DEnabled(value: Boolean) {
        _is3DEnabled.value = value
        viewModelScope.launch {
            pref.set3DEnabled(value)
        }
    }

    fun changeStableTrajectoryRatio(value: Int) {
        _stableTrajectorySensitivity.value = value
        viewModelScope.launch {
            pref.setStableTrajectoryRatio(value)
        }
    }

    fun changeVerticalMovementSensitivity(value: Int) {
        _verticalMovementSensitivity.value = value
        viewModelScope.launch {
            pref.setVerticalMovementSensitivity(value)
        }
    }

    fun changeHorizontalCompressionSensitivity(value: Int) {
        _horizontalCompressionSensitivity.value = value
        viewModelScope.launch {
            pref.setHorizontalCompressionSensitivity(value)
        }
    }

    fun changeAlarmUri(uri: Uri) {
        selectedAlarmUri = uri
        viewModelScope.launch {
            pref.setAlarmUri(uri)
        }
    }

    fun changeAllowStats(value: Boolean) {
        _allowStats.value = value
        viewModelScope.launch {
            pref.setAllowStats(value)
        }
    }

    fun changeFirstLaunch(value: Boolean) {
        _isFirstLaunch.value = value
        viewModelScope.launch {
            pref.setFirstLaunch(value)
        }
    }

    /**
     * Can initialize here something on first run
     */
    fun initDefaults() {
        val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        changeAlarmUri(defaultAlarmUri)
    }

    fun resetMapSettingsToDefault() {
        change3DEnabled(DEFAULT_3D_ENABLED)
    }

    fun resetAlarmSettingsToDefault() {
        changeAlarmUri(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        changeAutoDayNightDetect(DEFAULT_AUTO_DAY_NIGHT_DETECT)
        changeStableTrajectoryRatio(DEFAULT_STABLE_TRAJECTORY_SENSITIVITY)
        changeVerticalMovementSensitivity(DEFAULT_VERTICAL_MOVEMENT_SENSITIVITY)
        changeHorizontalCompressionSensitivity(DEFAULT_HORIZONTAL_COMPRESSION_SENSITIVITY)
    }
}
