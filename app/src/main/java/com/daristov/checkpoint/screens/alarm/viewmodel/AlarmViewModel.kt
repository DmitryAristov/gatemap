package com.daristov.checkpoint.screens.alarm.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.screens.alarm.AlarmUiState
import com.daristov.checkpoint.screens.alarm.detector.RearLightsDetector
import com.daristov.checkpoint.screens.alarm.detector.RearLightsMotionDetector
import com.daristov.checkpoint.screens.settings.DEFAULT_AUTO_DAY_NIGHT_DETECT
import com.daristov.checkpoint.screens.settings.DEFAULT_HORIZONTAL_COMPRESSION_SENSITIVITY
import com.daristov.checkpoint.screens.settings.DEFAULT_STABLE_TRAJECTORY_SENSITIVITY
import com.daristov.checkpoint.screens.settings.DEFAULT_VERTICAL_MOVEMENT_SENSITIVITY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.daristov.checkpoint.service.OrientationProvider
import com.daristov.checkpoint.screens.settings.SettingsPreferenceManager
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

    private val orientationProvider = OrientationProvider(application.applicationContext)
    private lateinit var motionDetector: RearLightsMotionDetector
    private val lightsDetector: RearLightsDetector = RearLightsDetector()

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState

    private var frameWidth: Int = 0
    private var frameHeight: Int = 0
    private var isAutoDayNightDetectEnabled: Boolean = DEFAULT_AUTO_DAY_NIGHT_DETECT
    private var stableTrajectoryRatio: Double = DEFAULT_STABLE_TRAJECTORY_SENSITIVITY * 0.01
    private var verticalMovementSensitivity: Double = DEFAULT_VERTICAL_MOVEMENT_SENSITIVITY * 0.01
    private var horizontalCompressionSensitivity: Double =
        DEFAULT_HORIZONTAL_COMPRESSION_SENSITIVITY * 0.01

    init {
        viewModelScope.launch {
            settingsManager.getAutoDayNightDetect().collect {
                isAutoDayNightDetectEnabled = it
            }
        }
        viewModelScope.launch {
            settingsManager.getStableTrajectoryRatio().collect {
                stableTrajectoryRatio = it.toFloat() * 0.01
            }
        }
        viewModelScope.launch {
            settingsManager.getVerticalMovementSensitivity().collect {
                verticalMovementSensitivity = it.toFloat() * 0.01
            }
        }
        viewModelScope.launch {
            settingsManager.getHorizontalCompressionSensitivity().collect {
                horizontalCompressionSensitivity = it.toFloat() * 0.01
            }
        }
    }

    fun handleImageProxy(imageProxy: ImageProxy) {
        val bitmap = ImageProxyUtils.toBitmap(imageProxy)

        if (frameWidth == 0 || frameHeight == 0) {
            frameWidth = bitmap.width
            frameHeight = bitmap.height
            motionDetector = RearLightsMotionDetector(
                frameWidth, frameHeight,
                verticalMovementSensitivity, horizontalCompressionSensitivity, stableTrajectoryRatio
            )
        }

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        val hsv = Mat()
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        val isNight =
            if (isAutoDayNightDetectEnabled) lightsDetector.isNightImage(hsv) else uiState.value.isNight
        _uiState.update {
            it.copy(
                isNight = isNight,
                bitmapSize = Size(bitmap.width, bitmap.height)
            )
        }

        analyzeFrame(bitmap)
        imageProxy.close()
    }

    fun analyzeFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val rearLightPair = lightsDetector.process(bitmap)
            rearLightPair?.let {
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
        _uiState.update {
            it.copy(
                pitch = pitch,
                roll = roll
            )
        }
    }

    fun setManualNightMode(isNight: Boolean) {
        _uiState.update {
            it.copy(
                isNight = isNight
            )
        }
    }

    fun resetMotionDetection() {
        _uiState.update { it.copy(motionDetected = false) }
    }
}

