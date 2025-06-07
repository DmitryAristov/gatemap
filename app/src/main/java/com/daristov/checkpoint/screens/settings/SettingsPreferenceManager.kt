package com.daristov.checkpoint.screens.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.core.net.toUri

val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsPreferenceManager(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val RINGTONE_KEY = stringPreferencesKey("alarm_ringtone_uri")
    private val SHRINK_SENSITIVITY_KEY = intPreferencesKey("shrink_sensitivity_int")
    private val RISE_SENSITIVITY_KEY = intPreferencesKey("rise_sensitivity_int")
    private val STABLE_TRAJECTORY_SENSITIVITY_KEY = intPreferencesKey("stable_trajectory_sensitivity_int")
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

    fun getShrinkSensitivity(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SHRINK_SENSITIVITY_KEY] ?: 100
    }

    suspend fun setShrinkSensitivity(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[SHRINK_SENSITIVITY_KEY] = value
        }
    }

    fun getRiseSensitivity(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[RISE_SENSITIVITY_KEY] ?: 100
    }

    suspend fun setRiseSensitivity(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[RISE_SENSITIVITY_KEY] = value
        }
    }

    fun getStableTrajectoryRatio(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[STABLE_TRAJECTORY_SENSITIVITY_KEY] ?: 100
    }

    suspend fun setStableTrajectoryRatio(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[STABLE_TRAJECTORY_SENSITIVITY_KEY] = value
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

