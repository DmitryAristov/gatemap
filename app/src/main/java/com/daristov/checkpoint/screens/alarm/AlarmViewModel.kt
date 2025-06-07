package com.daristov.checkpoint.screens.alarm

import android.app.Application
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.detector.RearLightsDetector
import com.daristov.checkpoint.detector.RearLightsMotionDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.daristov.checkpoint.detector.VehicleDetector
import com.daristov.checkpoint.sensor.OrientationProvider
import com.daristov.checkpoint.ui.components.SettingsPreferenceManager
import com.daristov.checkpoint.util.ImageProxyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class AlarmViewModel(
    application: Application,
    private val settingsManager: SettingsPreferenceManager
) : AndroidViewModel(application), OrientationProvider.Listener {

    private val vehicleDetector = VehicleDetector()
    private val orientationProvider = OrientationProvider(application.applicationContext)
    private var sensitivity = 0.7  // по умолчанию 70%
    private val motionDetector = RearLightsMotionDetector(sensitivityThreshold = sensitivity)

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsManager.getDetectionSensitivity().collect {
                sensitivity = it * 0.01
                motionDetector.setSensitivity(sensitivity)
            }
        }
    }
    fun handleImageProxy(imageProxy: ImageProxy) {
        val bitmap = ImageProxyUtils.toBitmap(imageProxy)

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        val hsv = Mat()
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        val night = RearLightsDetector.isNightImage(hsv)
        _uiState.update {
            it.copy(isNight = night, bitmapSize = Size(bitmap.width, bitmap.height))
        }

        analyzeFrame(bitmap)
        imageProxy.close()
    }

    fun analyzeFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val rearLightPair = vehicleDetector.process(bitmap)
            if (rearLightPair != null) {
                val motionDetected = motionDetector.update(rearLightPair)
                _uiState.update {
                    it.copy(
                        lastDetectedRearLights = rearLightPair,
                        motionDetected = motionDetected
                    )
                }
            }
        }
    }

    fun startOrientationTracking() {
        orientationProvider.start(this)
    }

    fun stopOrientationTracking() {
        orientationProvider.stop()
    }

    override fun onOrientationChanged(pitch: Float, roll: Float, azimuth: Float) {
        _uiState.update { it.copy(pitch = pitch, roll = roll) }
    }
}

