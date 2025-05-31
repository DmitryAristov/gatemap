package com.daristov.checkpoint.screens.alarm

import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.daristov.checkpoint.detector.VehicleDetector
import com.daristov.checkpoint.util.ImageProxyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlarmViewModel : ViewModel() {

    private val vehicleDetector: VehicleDetector = VehicleDetector()
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
            val rearLights = vehicleDetector.process(bitmap)
            if (rearLights.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        lastDetectedRearLights = rearLights,
                    )
                }
            }
        }
    }
}

