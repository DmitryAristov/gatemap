package com.daristov.checkpoint.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AppThemeMode(val label: String) {
    LIGHT("Светлая"),
    DARK("Темная"),
    SYSTEM("Системная");

    override fun toString(): String = label
}

enum class AppLanguage(val label: String) {
    RU("Русский"),
    KZ("Қазақша"),
    EN("English");

    override fun toString(): String = label
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val pref = SettingsPreferenceManager(application.applicationContext)

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    private val _language = MutableStateFlow(AppLanguage.RU)
    private val _autoDayNightDetect = MutableStateFlow(true)
    private val _is3DEnabled = MutableStateFlow(true)
    private val _stableTrajectorySensitivity = MutableStateFlow(50)
    private val _verticalMovementSensitivity = MutableStateFlow(50)
    private val _horizontalCompressionSensitivity = MutableStateFlow(50)
    private val _allowStats = MutableStateFlow(true)
    private val _isFirstLaunch = MutableStateFlow(true)

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

    fun initDefaults() {
        val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        changeAlarmUri(defaultAlarmUri)
        /*
         * Can initialize here something on first run
         */
    }
}
