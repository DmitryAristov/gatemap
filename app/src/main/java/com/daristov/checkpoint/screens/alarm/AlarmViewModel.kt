package com.daristov.checkpoint.screens.alarm

import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.daristov.checkpoint.detector.VehicleDetector
import com.daristov.checkpoint.detector.tracking.VehicleTracker
import com.daristov.checkpoint.util.ImageProxyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlarmViewModel : ViewModel() {

    private val vehicleDetector: VehicleDetector = VehicleDetector()
    private val _uiState = MutableStateFlow(AlarmUiState())
    private val vehicleTracker = VehicleTracker()
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
            val box = vehicleDetector.process(bitmap)
            if (box != null) {
//                _uiState.update {
//                    it.copy(
//                        detectedLines = box,
//                        isImageStable = true
//                    )
//                }

                val isStable = vehicleTracker.update(box)

                if (isStable) {
                    _uiState.update {
                        it.copy(
                            lastDetectedBox = vehicleTracker.currentStableBox,
                            isImageStable = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            lastDetectedBox = null,
                            isImageStable = false
                        )
                    }
                }
            }
        }
    }
}

