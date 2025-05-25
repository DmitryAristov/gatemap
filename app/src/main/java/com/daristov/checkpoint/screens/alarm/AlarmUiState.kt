package com.daristov.checkpoint.screens.alarm

import android.util.Size
import com.daristov.checkpoint.domain.model.Line
import com.daristov.checkpoint.domain.model.QuadBox

data class AlarmUiState(
    val lastDetectedBox: QuadBox? = null,
    val detectedLines: List<Line> = emptyList(),
    val isImageStable: Boolean = false,
    val isProcessing: Boolean = false,
    val motionDetected: Boolean = false,
    val isAutoAnalysisEnabled: Boolean = true,
    val bitmapSize: Size? = null
)