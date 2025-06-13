package com.daristov.checkpoint.screens.alarm

import android.util.Size
import com.daristov.checkpoint.screens.alarm.detector.RearLightsDetector.RearLightPair

data class AlarmUiState(
    val lastDetectedRearLights: RearLightPair? = null,
    val motionDetected: Boolean = false,
    val bitmapSize: Size? = null,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isNight: Boolean? = null
)