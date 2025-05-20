package com.daristov.checkpoint.ui.components

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daristov.checkpoint.screens.settings.AppLanguage
import com.daristov.checkpoint.screens.settings.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.core.net.toUri

val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsPreferenceManager(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val RINGTONE_KEY = stringPreferencesKey("alarm_ringtone_uri")
    private val SENSITIVITY_KEY = intPreferencesKey("detection_sensitivity_int")
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
        prefs[RINGTONE_KEY]?.toUri()
    }

    suspend fun setAlarmUri(uri: Uri?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[RINGTONE_KEY] = uri.toString()
            else prefs.remove(RINGTONE_KEY)
        }
    }

    fun getDetectionSensitivity(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SENSITIVITY_KEY] ?: 70
    }

    suspend fun setDetectionSensitivity(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[SENSITIVITY_KEY] = value
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

