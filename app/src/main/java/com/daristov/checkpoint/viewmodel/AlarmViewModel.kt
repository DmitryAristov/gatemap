package com.daristov.checkpoint.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.daristov.checkpoint.detector.MotionDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import androidx.core.graphics.createBitmap

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

    private var _detectedContour: MatOfPoint? = null
    val detectedContour: MatOfPoint?
        get() = _detectedContour

    private val _overlayBitmap = MutableStateFlow<Bitmap?>(null)
    val overlayBitmap: StateFlow<Bitmap?> = _overlayBitmap

    fun recalibrate() {
        _state.value = _state.value.copy(referenceBitmap = null, motionDetected = false)
    }

    fun setStep(step: CalibrationStep) {
        _calibrationStep.value = step
    }

    fun onFrame(input: Bitmap) {
        Log.d("Camera", "onFrame method called")
        if (calibrationStep.value != CalibrationStep.SEARCHING_CONTOURS) return

        val src = Mat()
        Utils.bitmapToMat(input, src)

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(src, src, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(src, src, 50.0, 150.0)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(src, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val largest = contours.maxByOrNull { Imgproc.contourArea(it) }
        Log.d("Camera", "Contours found: ${contours.size}, best area = ${Imgproc.contourArea(largest)}")

        if (largest != null && Imgproc.contourArea(largest) > 1000.0) {
            _detectedContour = largest

            // Нарисовать контур на Bitmap
            val overlayMat = Mat.zeros(src.size(), CvType.CV_8UC4)
            Imgproc.drawContours(overlayMat, listOf(largest), -1, Scalar(0.0, 255.0, 0.0, 255.0), 4)
            val bmp = createBitmap(overlayMat.cols(), overlayMat.rows())
            Utils.matToBitmap(overlayMat, bmp)
            _overlayBitmap.value = bmp

            setStep(CalibrationStep.WAITING_USER_CONFIRMATION)
        }
    }


}
