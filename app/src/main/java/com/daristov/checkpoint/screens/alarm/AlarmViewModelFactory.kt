package com.daristov.checkpoint.screens.alarm

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.daristov.checkpoint.ui.components.SettingsPreferenceManager

class AlarmViewModelFactory(
    private val application: Application,
    private val settingsManager: SettingsPreferenceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(application, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
