package com.daristov.checkpoint.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AppThemeMode { LIGHT, DARK, SYSTEM }
enum class AppLanguage { RU, KZ, EN }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val pref = SettingsPreferenceManager(application.applicationContext)
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    private val _verticalMovementSensitivity = MutableStateFlow(50)
    private val _horizontalCompressionSensitivity = MutableStateFlow(50)
    private val _stableTrajectorySensitivity = MutableStateFlow(50)
    private val _language = MutableStateFlow(AppLanguage.RU)

    val themeMode: StateFlow<AppThemeMode> = _themeMode
    var selectedAlarmUri by mutableStateOf<Uri?>(null)
        private set
    val verticalMovementSensitivity: StateFlow<Int> = _verticalMovementSensitivity
    val horizontalCompressionSensitivity: StateFlow<Int> = _horizontalCompressionSensitivity
    val stableTrajectorySensitivity: StateFlow<Int> = _stableTrajectorySensitivity
    val language: StateFlow<AppLanguage> = _language

    init {
        viewModelScope.launch {
            pref.getTheme().collect { _themeMode.value = it }
        }

        viewModelScope.launch {
            pref.getAlarmUri().collect { selectedAlarmUri = it }
        }

        viewModelScope.launch {
            pref.getHorizontalCompressionSensitivity().collect { _horizontalCompressionSensitivity.value = it }
        }

        viewModelScope.launch {
            pref.getVerticalMovementSensitivity().collect { _verticalMovementSensitivity.value = it }
        }

        viewModelScope.launch {
            pref.getStableTrajectoryRatio().collect { _stableTrajectorySensitivity.value = it }
        }

        viewModelScope.launch {
            pref.getLanguage().collect { _language.value = it }
        }
    }

    fun changeTheme(mode: AppThemeMode) {
        viewModelScope.launch { pref.setTheme(mode) }
    }

    fun changeAlarmUri(uri: Uri) {
        selectedAlarmUri = uri
        viewModelScope.launch { pref.setAlarmUri(uri) }
    }

    fun changeLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            pref.setLanguage(lang)
        }
    }

    fun changeVerticalMovementSensitivity(value: Int) {
        _verticalMovementSensitivity.value = value
        viewModelScope.launch { pref.setVerticalMovementSensitivity(value) }
    }

    fun changeHorizontalCompressionSensitivity(value: Int) {
        _horizontalCompressionSensitivity.value = value
        viewModelScope.launch { pref.setHorizontalCompressionSensitivity(value) }
    }

    fun changeStableTrajectoryRatio(value: Int) {
        _stableTrajectorySensitivity.value = value
        viewModelScope.launch { pref.setStableTrajectoryRatio(value) }
    }
}


