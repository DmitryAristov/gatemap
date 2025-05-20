package com.daristov.checkpoint.screens.alarm

import com.daristov.checkpoint.domain.model.QuadBox

data class AlarmUiState(
    val lastDetectedBox: QuadBox? = null,
    val isImageStable: Boolean = false,
    val isProcessing: Boolean = false,
    val motionDetected: Boolean = false
)