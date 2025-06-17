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
    private val VERTICAL_MOVEMENT_SENSITIVITY_KEY = intPreferencesKey("vertical_movement_sensitivity_int")
    private val HORIZONTAL_COMPRESSION_SENSITIVITY_KEY = intPreferencesKey("horizontal_compression_sensitivity_int")
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

    fun getVerticalMovementSensitivity(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[VERTICAL_MOVEMENT_SENSITIVITY_KEY] ?: 50
    }

    suspend fun setVerticalMovementSensitivity(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[VERTICAL_MOVEMENT_SENSITIVITY_KEY] = value
        }
    }

    fun getHorizontalCompressionSensitivity(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[HORIZONTAL_COMPRESSION_SENSITIVITY_KEY] ?: 50
    }

    suspend fun setHorizontalCompressionSensitivity(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[HORIZONTAL_COMPRESSION_SENSITIVITY_KEY] = value
        }
    }

    fun getStableTrajectoryRatio(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[STABLE_TRAJECTORY_SENSITIVITY_KEY] ?: 50
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

