package com.daristov.checkpoint.ui.components

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daristov.checkpoint.viewmodel.AppLanguage
import com.daristov.checkpoint.viewmodel.AppThemeMode
import com.daristov.checkpoint.viewmodel.DetectionSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsPreferenceManager(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val RINGTONE_KEY = stringPreferencesKey("alarm_ringtone_uri")
    private val SENSITIVITY_KEY = stringPreferencesKey("detection_sensitivity")
    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    fun getTheme(): Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    suspend fun setTheme(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }

    fun getAlarmUri(): Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[RINGTONE_KEY]?.let { Uri.parse(it) }
    }

    suspend fun setAlarmUri(uri: Uri?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[RINGTONE_KEY] = uri.toString()
            else prefs.remove(RINGTONE_KEY)
        }
    }

    fun getDetectionSensitivity(): Flow<DetectionSensitivity> = context.dataStore.data.map { prefs ->
        when (prefs[SENSITIVITY_KEY]) {
            "LOW" -> DetectionSensitivity.LOW
            "HIGH" -> DetectionSensitivity.HIGH
            else -> DetectionSensitivity.MEDIUM
        }
    }

    suspend fun setDetectionSensitivity(s: DetectionSensitivity) {
        context.dataStore.edit { prefs ->
            prefs[SENSITIVITY_KEY] = s.name
        }
    }

    fun getLanguage(): Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[LANGUAGE_KEY]) {
            "KZ" -> AppLanguage.KZ
            "EN" -> AppLanguage.EN
            else -> AppLanguage.RU
        }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = lang.name
        }
    }


}

