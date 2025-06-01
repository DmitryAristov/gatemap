package com.daristov.checkpoint.screens.alarm

import android.app.Application
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.detector.RearLightsDetector.RearLightPair
import com.daristov.checkpoint.detector.RearLightsMotionDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.daristov.checkpoint.detector.VehicleDetector
import com.daristov.checkpoint.sensor.OrientationProvider
import com.daristov.checkpoint.util.ImageProxyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application), OrientationProvider.Listener {
    private val orientationProvider = OrientationProvider(application.applicationContext)
    private val vehicleDetector: VehicleDetector = VehicleDetector()
    private val rearLightsMotionDetector = RearLightsMotionDetector()
    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState

    fun handleImageProxy(imageProxy: ImageProxy) {
        val bitmap = ImageProxyUtils.toBitmap(imageProxy)
        _uiState.update {
            it.copy(bitmapSize = Size(bitmap.width, bitmap.height))
        }
        analyzeFrame(bitmap)
        imageProxy.close()
    }

    fun analyzeFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val rearLightPair = vehicleDetector.process(bitmap)
            if (rearLightPair != null) {
                val motionDetected = rearLightsMotionDetector.update(rearLightPair)
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

