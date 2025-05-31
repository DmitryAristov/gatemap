package com.daristov.checkpoint.screens.alarm

import android.util.Size
import org.opencv.core.Rect

data class AlarmUiState(
    val lastDetectedRearLights: List<Rect> = emptyList(),
    val motionDetected: Boolean = false,
    val bitmapSize: Size? = null
)