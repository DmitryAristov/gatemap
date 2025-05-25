package com.daristov.checkpoint.screens.alarm

import android.content.Context
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

    private val imageStabilityDetector: ImageStabilityDetector = ImageStabilityDetector()
    private val vehicleDetector: VehicleDetector = VehicleDetector()
    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState


    fun handleImageProxy(imageProxy: ImageProxy) {
        try {
            val bitmap = ImageProxyUtils.toBitmap(imageProxy)
            _uiState.update {
                it.copy(bitmapSize = Size(bitmap.width, bitmap.height))
            }
            analyzeFrame(bitmap)
        } finally {
            imageProxy.close() // закрываем строго после обработки
        }
    }

    fun analyzeFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val isStable = imageStabilityDetector.compareWithPrevious(bitmap)

            if (!isStable) {
                // изображение поменялось — сброс
                _uiState.update { it.copy(lastDetectedBox = null, isImageStable = false) }

                val box = vehicleDetector.process(bitmap)
                if (box != null) {
                    _uiState.update {
                        it.copy(
                            lastDetectedBox = box,
                            isImageStable = true
                        )
                    }
                }

            } else {
                // изображение стабильное — ничего не делаем, просто ждём подтверждения
            }
        }
    }
}

