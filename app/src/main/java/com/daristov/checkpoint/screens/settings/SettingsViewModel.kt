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
    private val _shrinkSensitivity = MutableStateFlow(100)
    private val _riseSensitivity = MutableStateFlow(100)
    private val _stableTrajectorySensitivity = MutableStateFlow(100)
    private val _language = MutableStateFlow(AppLanguage.RU)

    val themeMode: StateFlow<AppThemeMode> = _themeMode
    var selectedAlarmUri by mutableStateOf<Uri?>(null)
        private set
    val shrinkSensitivity: StateFlow<Int> = _shrinkSensitivity
    val riseSensitivity: StateFlow<Int> = _riseSensitivity
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
            pref.getRiseSensitivity().collect { _riseSensitivity.value = it }
        }

        viewModelScope.launch {
            pref.getShrinkSensitivity().collect { _shrinkSensitivity.value = it }
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

    fun changeShrinkSensitivity(value: Int) {
        _shrinkSensitivity.value = value
        viewModelScope.launch { pref.setShrinkSensitivity(value) }
    }

    fun changeRiseSensitivity(value: Int) {
        _riseSensitivity.value = value
        viewModelScope.launch { pref.setRiseSensitivity(value) }
    }

    fun changeStableTrajectoryRatio(value: Int) {
        _stableTrajectorySensitivity.value = value
        viewModelScope.launch { pref.setStableTrajectoryRatio(value) }
    }
}


