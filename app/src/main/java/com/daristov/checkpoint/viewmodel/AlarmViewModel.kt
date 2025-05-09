package com.daristov.checkpoint.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.daristov.checkpoint.detector.MotionDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AlarmUiState(
    val motionDetected: Boolean = false,
    val referenceBitmap: Bitmap? = null
)

enum class CalibrationStep {
    WAITING_FOR_CAMERA,
    AUTO_ADJUSTING,
    SEARCHING_CONTOURS,
    WAITING_USER_CONFIRMATION,
    TRACKING,
    TRIGGERED
}

class AlarmViewModel : ViewModel() {
    private val _state = MutableStateFlow(AlarmUiState())
    val state: StateFlow<AlarmUiState> = _state

    private val _calibrationStep = MutableStateFlow(CalibrationStep.WAITING_FOR_CAMERA)
    val calibrationStep: StateFlow<CalibrationStep> = _calibrationStep

    fun processFrame(frame: Bitmap) {
        val reference = _state.value.referenceBitmap ?: return
        val motion = MotionDetector.detectMotion(frame, reference) // <-- TODO: OpenCV

        _state.value = _state.value.copy(motionDetected = motion)
    }

    fun recalibrate() {
        _state.value = _state.value.copy(referenceBitmap = null, motionDetected = false)
    }

    fun setReference(bitmap: Bitmap) {
        _state.value = _state.value.copy(referenceBitmap = bitmap)
    }

    fun setStep(step: CalibrationStep) {
        _calibrationStep.value = step
    }

}
