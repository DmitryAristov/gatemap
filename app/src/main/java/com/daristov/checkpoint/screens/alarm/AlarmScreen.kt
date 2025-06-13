package com.daristov.checkpoint.screens.alarm

import android.app.Application
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.ScaleType.FIT_CENTER
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.daristov.checkpoint.screens.alarm.detector.RearLightsDetector
import com.daristov.checkpoint.screens.alarm.viewmodel.AlarmViewModel
import com.daristov.checkpoint.screens.alarm.viewmodel.AlarmViewModelFactory
import com.daristov.checkpoint.screens.settings.SettingsPreferenceManager
import java.util.concurrent.Executors

@Composable
fun AlarmScreen(navController: NavHostController) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val settingsManager = remember { SettingsPreferenceManager(context) }
    val factory = remember { AlarmViewModelFactory(application, settingsManager) }
    val viewModel: AlarmViewModel = viewModel(factory = factory)

    val state by viewModel.uiState.collectAsState()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding, bottom = navBarPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel
            )

            val bitmapSize = state.bitmapSize
            val rearLights = state.lastDetectedRearLights

            if (rearLights != null && bitmapSize != null) {
                DrawRearLightsOverlay(
                    rects = rearLights,
                    bitmapWidth = bitmapSize.width.toInt(),
                    bitmapHeight = bitmapSize.height.toInt()
                )
            }
        }

        // Нижняя панель
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 150.dp)
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.motionDetected) "🚨 Обнаружено движение!" else "🟢 Ожидает",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (state.isNight == true) "🌙 Ночь" else "☀️ День",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )
            }
        }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    viewModel: AlarmViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = FIT_CENTER
            setBackgroundColor(backgroundColor)
            setOnApplyWindowInsetsListener { v, insets ->
                v.onApplyWindowInsets(insets)
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    )

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Настройка Preview с Camera2Interop
            val previewBuilder = Preview.Builder()
            val camera2Ext = Camera2Interop.Extender(previewBuilder)

            camera2Ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )
            camera2Ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            camera2Ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CameraMetadata.CONTROL_AWB_MODE_AUTO
            )
            camera2Ext.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )

            val preview = previewBuilder
                .build()
                .apply {
                    previewView.scaleType = FIT_CENTER
                    surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        viewModel::handleImageProxy
                    )
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        viewModel.startOrientationTracking()
        onDispose {
            viewModel.stopOrientationTracking()
        }
    }
}

@Composable
fun DrawRearLightsOverlay(
    rects: RearLightsDetector.RearLightPair,
    bitmapWidth: Int,
    bitmapHeight: Int,
    color: Color = Color.Red,
    strokeWidth: Float = 12f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Масштаб с сохранением пропорций
        val scale = minOf(size.width / bitmapWidth, size.height / bitmapHeight)

        // Размер отмасштабированного изображения
        val scaledWidth = bitmapWidth * scale
        val scaledHeight = bitmapHeight * scale

        // Смещение, чтобы центрировать изображение
        val offsetX = (size.width - scaledWidth) / 2f
        val offsetY = (size.height - scaledHeight) / 2f

        for (rect in listOf(rects.left, rects.right)) {
            val left = rect.x * scale + offsetX
            val top = rect.y * scale + offsetY
            val right = (rect.x + rect.width) * scale + offsetX
            val bottom = (rect.y + rect.height) * scale + offsetY

            drawRect(
                color = color,
                topLeft = Offset(left.toFloat(), top.toFloat()),
                size = Size(
                    (right - left).toFloat(),
                    (bottom - top).toFloat()
                ),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}