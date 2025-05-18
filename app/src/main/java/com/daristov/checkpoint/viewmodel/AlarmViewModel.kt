package com.daristov.checkpoint.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import com.daristov.checkpoint.detector.ImageProcessor
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

data class AlarmUiState(
    val motionDetected: Boolean = false,
    val referenceBitmap: Bitmap? = null,
    val truckBox: ImageProcessor.QuadBox? = null
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

    private val _overlayBitmap = MutableStateFlow<Bitmap?>(null)
    val overlayBitmap: StateFlow<Bitmap?> = _overlayBitmap
    private val processor = ImageProcessor()

    fun recalibrate() {
        _state.value = _state.value.copy(referenceBitmap = null, motionDetected = false)
    }

    fun setStep(step: CalibrationStep) {
        _calibrationStep.value = step
    }

    fun onFrame(input: Bitmap) {
        if (calibrationStep.value != CalibrationStep.SEARCHING_CONTOURS) return

        val box = processor.process(input)

        if (box != null) {
            _state.value = _state.value.copy(truckBox = box)

            // Нарисовать рамку
            val mat = Mat()
            Utils.bitmapToMat(input, mat)

            val resultMat = drawQuadBox(mat, box)
            val bmp = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(resultMat, bmp)
            _overlayBitmap.value = bmp

            setStep(CalibrationStep.WAITING_USER_CONFIRMATION)
        }
    }

    fun drawQuadBox(image: Mat, box: ImageProcessor.QuadBox, color: Scalar = Scalar(255.0, 255.0, 0.0), thickness: Int = 3): Mat {
        val output = image.clone()
        val points = listOf(box.topLeft, box.topRight, box.bottomRight, box.bottomLeft)

        for (i in points.indices) {
            val pt1 = points[i]
            val pt2 = points[(i + 1) % points.size]
            Imgproc.line(output, pt1, pt2, color, thickness)
        }

        return output
    }
}
