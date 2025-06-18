package com.daristov.checkpoint.screens.alarm

import android.app.Application
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daristov.checkpoint.R
import com.daristov.checkpoint.screens.alarm.detector.RearLightsDetector
import com.daristov.checkpoint.screens.alarm.viewmodel.AlarmViewModel
import com.daristov.checkpoint.screens.alarm.viewmodel.AlarmViewModelFactory
import com.daristov.checkpoint.screens.settings.SettingsPreferenceManager
import com.daristov.checkpoint.screens.settings.SettingsViewModel
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val settingsManager = remember { SettingsPreferenceManager(context) }
    val factory = remember { AlarmViewModelFactory(application, settingsManager) }
    val viewModel: AlarmViewModel = viewModel(factory = factory)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.motion_sensor),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenMenu
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                }
            )
        }
    ) { padding ->
        AlarmContainer(viewModel, padding)
    }

    BackHandler { onBack() }
}

@Composable
fun AlarmContainer(
    viewModel: AlarmViewModel,
    padding: PaddingValues,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isAutoDayNightDetectEnabled = settingsViewModel.autoDayNightDetect.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
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
                    text = if (state.motionDetected)
                        "🚨 ${stringResource(R.string.motion_detected)}!"
                    else
                        "🟢 ${stringResource(R.string.waiting)}",
                    style = MaterialTheme.typography.titleMedium
                )

                if (isAutoDayNightDetectEnabled.value) {
                    Text(
                        text = if (state.isNight == true) "🌙 ${stringResource(R.string.night)}" else "☀️ ${stringResource(R.string.day)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${stringResource(R.string.mode)}:", style = MaterialTheme.typography.bodyMedium)
                        SegmentedDayNightSwitch(
                            isNight = state.isNight ?: false,
                            onChange = { viewModel.setManualNightMode(it) }
                        )
                    }
                }
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

        val leftRect = rects.left
        val rightRect = rects.right

        val leftBottom = Offset(
            leftRect.x * scale + offsetX - 50f,
            (leftRect.y + leftRect.height) * scale + offsetY
        )
        val rightBottom = Offset(
            (rightRect.x + rightRect.width) * scale + offsetX + 50f,
            (rightRect.y + rightRect.height) * scale + offsetY
        )

        val squareWidth = rightBottom.x - leftBottom.x
        val squareHeight = squareWidth // квадрат
        val cornerRadius = 100f

        val topLeft = Offset(leftBottom.x, leftBottom.y - squareHeight)
        val topRight = Offset(rightBottom.x, rightBottom.y - squareHeight)

        val path = Path().apply {
            moveTo(topLeft.x + cornerRadius, topLeft.y)

            // Верхняя сторона с правым верхним скруглением
            lineTo(topRight.x - cornerRadius, topRight.y)
            arcTo(
                rect = Rect(
                    topRight.x - 2 * cornerRadius,
                    topRight.y,
                    topRight.x,
                    topRight.y + 2 * cornerRadius
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Правая сторона с правым нижним скруглением
            lineTo(rightBottom.x, rightBottom.y - cornerRadius)
            arcTo(
                rect = Rect(
                    rightBottom.x - 2 * cornerRadius,
                    rightBottom.y - 2 * cornerRadius,
                    rightBottom.x,
                    rightBottom.y
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Нижняя сторона с левым нижним скруглением
            lineTo(leftBottom.x + cornerRadius, leftBottom.y)
            arcTo(
                rect = Rect(
                    leftBottom.x,
                    leftBottom.y - 2 * cornerRadius,
                    leftBottom.x + 2 * cornerRadius,
                    leftBottom.y
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Левая сторона с левым верхним скруглением
            lineTo(topLeft.x, topLeft.y + cornerRadius)
            arcTo(
                rect = Rect(
                    topLeft.x,
                    topLeft.y,
                    topLeft.x + 2 * cornerRadius,
                    topLeft.y + 2 * cornerRadius
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }

        drawPath(
            path = path,
            color = color.copy(alpha = 0.35f),
            style = Fill
        )

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

@Composable
fun SegmentedDayNightSwitch(
    isNight: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

        Text(
            text = "☀️ ${stringResource(R.string.day)}",
            modifier = Modifier
                .clickable { onChange(false) }
                .background(if (!isNight) selectedColor else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (!isNight) Color.White else unselectedColor
        )

        Text(
            text = "🌙 ${stringResource(R.string.night)}",
            modifier = Modifier
                .clickable { onChange(true) }
                .background(if (isNight) selectedColor else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (isNight) Color.White else unselectedColor
        )
    }
}
