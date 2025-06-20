package com.daristov.checkpoint.screens.settings

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey

val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsPreferenceManager(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("app_theme")
    fun getTheme(): Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> DEFAULT_THEME
        }
    }
    suspend fun setTheme(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }

    private val LANGUAGE_KEY = stringPreferencesKey("app_language")
    fun getLanguage(): Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[LANGUAGE_KEY]) {
            "KZ" -> AppLanguage.KZ
            "EN" -> AppLanguage.EN
            else -> DEFAULT_LANGUAGE
        }
    }
    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = lang.name
        }
    }

    private val AUTO_DAY_NIGHT_DETECT_KEY = booleanPreferencesKey("auto_day_night_detect")
    fun getAutoDayNightDetect(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_DAY_NIGHT_DETECT_KEY] ?: DEFAULT_AUTO_DAY_NIGHT_DETECT
    }
    suspend fun setAutoDayNightDetect(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_DAY_NIGHT_DETECT_KEY] = value
        }
    }

    private val IS_3D_ENABLED_KEY = booleanPreferencesKey("is_3d_enabled")
    fun is3DEnabled(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_3D_ENABLED_KEY] ?: DEFAULT_3D_ENABLED
    }
    suspend fun set3DEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_3D_ENABLED_KEY] = value
        }
    }

    private val STABLE_TRAJECTORY_SENSITIVITY_KEY = intPreferencesKey("stable_trajectory_sensitivity_int")
    fun getStableTrajectoryRatio(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[STABLE_TRAJECTORY_SENSITIVITY_KEY] ?: DEFAULT_STABLE_TRAJECTORY_SENSITIVITY
    }
    suspend fun setStableTrajectoryRatio(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[STABLE_TRAJECTORY_SENSITIVITY_KEY] = value
        }
    }

    private val VERTICAL_MOVEMENT_SENSITIVITY_KEY = intPreferencesKey("vertical_movement_sensitivity_int")
    fun getVerticalMovementSensitivity(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[VERTICAL_MOVEMENT_SENSITIVITY_KEY] ?: DEFAULT_VERTICAL_MOVEMENT_SENSITIVITY
    }
    suspend fun setVerticalMovementSensitivity(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[VERTICAL_MOVEMENT_SENSITIVITY_KEY] = value
        }
    }

    private val HORIZONTAL_COMPRESSION_SENSITIVITY_KEY = intPreferencesKey("horizontal_compression_sensitivity_int")
    fun getHorizontalCompressionSensitivity(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[HORIZONTAL_COMPRESSION_SENSITIVITY_KEY] ?: DEFAULT_HORIZONTAL_COMPRESSION_SENSITIVITY
    }
    suspend fun setHorizontalCompressionSensitivity(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[HORIZONTAL_COMPRESSION_SENSITIVITY_KEY] = value
        }
    }

    private val RINGTONE_KEY = stringPreferencesKey("alarm_ringtone_uri")
    fun getAlarmUri(): Flow<Uri> = context.dataStore.data.map { prefs ->
        if (prefs.contains(RINGTONE_KEY)) {
            return@map prefs[RINGTONE_KEY]!!.toUri()
        } else {
            return@map RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }
    suspend fun setAlarmUri(uri: Uri) {
        context.dataStore.edit { prefs ->
            prefs[RINGTONE_KEY] = uri.toString()
        }
    }

    private val ALLOW_STATS_KEY = booleanPreferencesKey("allow_stats")
    fun getAllowStats(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ALLOW_STATS_KEY] ?: DEFAULT_ALLOW_STATS
    }
    suspend fun setAllowStats(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ALLOW_STATS_KEY] = value
        }
    }

    private val IS_FIRST_LAUNCH_KEY = booleanPreferencesKey("app_first_launch")
    fun isFirstLaunch(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_FIRST_LAUNCH_KEY] ?: DEFAULT_FIRST_LAUNCH
    }
    suspend fun setFirstLaunch(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_FIRST_LAUNCH_KEY] = value
        }
    }

    private val TRACKING_MODE_KEY = stringPreferencesKey("app_tracking_mode")
    fun getTrackingMode(): Flow<AppTrackingMode> = context.dataStore.data.map { prefs ->
        when (prefs[TRACKING_MODE_KEY]) {
            "COMPASS" -> AppTrackingMode.COMPASS
            "GPS" -> AppTrackingMode.GPS
            else -> DEFAULT_TRACKING_MODE
        }
    }
    suspend fun setTrackingMode(mode: AppTrackingMode) {
        context.dataStore.edit { prefs ->
            prefs[TRACKING_MODE_KEY] = mode.name
        }
    }
}

