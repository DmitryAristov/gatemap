package com.daristov.checkpoint.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.ui.components.SettingsPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AppThemeMode { LIGHT, DARK, SYSTEM }
enum class AppLanguage { RU, KZ, EN }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val pref = SettingsPreferenceManager(application.applicationContext)
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    private val _sensitivity = MutableStateFlow(70)
    private val _language = MutableStateFlow(AppLanguage.RU)

    val themeMode: StateFlow<AppThemeMode> = _themeMode
    var selectedAlarmUri by mutableStateOf<Uri?>(null)
        private set
    val sensitivity: StateFlow<Int> = _sensitivity
    val language: StateFlow<AppLanguage> = _language

    init {
        viewModelScope.launch {
            pref.getTheme().collect { _themeMode.value = it }
        }

        viewModelScope.launch {
            pref.getAlarmUri().collect { selectedAlarmUri = it }
        }

        viewModelScope.launch {
            pref.getDetectionSensitivity().collect { _sensitivity.value = it }
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

    fun changeSensitivity(value: Int) {
        _sensitivity.value = value
        viewModelScope.launch { pref.setDetectionSensitivity(value) }
    }

}


