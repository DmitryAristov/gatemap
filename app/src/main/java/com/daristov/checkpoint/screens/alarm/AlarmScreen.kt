package com.daristov.checkpoint.screens.alarm

import android.app.Application
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.max
import kotlin.math.roundToInt

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
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var alarmTriggered by remember { mutableStateOf(false) }
    val isAutoDayNightDetectEnabled = settingsViewModel.autoDayNightDetect.collectAsState()
    var ringtone by remember { mutableStateOf<Ringtone?>(null) }

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

        AlarmBottomStatusPanel(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            state = state,
            isAutoDayNightDetectEnabled = isAutoDayNightDetectEnabled,
            viewModel = viewModel
        )
    }

    if (alarmTriggered) {
        AlarmTriggeredOverlay(
            onDismiss = {
                ringtone?.stop()
                alarmTriggered = false
                viewModel.resetMotionDetection()
            }
        )
    }

    LaunchedEffect(state.motionDetected) {
        if (state.motionDetected && !alarmTriggered) {
            alarmTriggered = true
            ringtone = RingtoneManager.getRingtone(context, settingsViewModel.selectedAlarmUri)
            ringtone?.play()
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
                    previewView.scaleType = FILL_CENTER
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
fun AlarmBottomStatusPanel(
    modifier: Modifier = Modifier,
    state: AlarmUiState,
    isAutoDayNightDetectEnabled: State<Boolean>,
    viewModel: AlarmViewModel
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Блок статуса (движение/ожидание)
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (state.motionDetected)
                        "🚨 ${stringResource(R.string.motion_detected)}!"
                    else
                        "🟢 ${stringResource(R.string.waiting)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val isNight = state.isNight ?: false
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable(
                        enabled = !isAutoDayNightDetectEnabled.value,
                        interactionSource = interactionSource,
                        indication = rememberRipple()
                    ) {
                        if (!isAutoDayNightDetectEnabled.value) {
                            viewModel.setManualNightMode(!isNight)
                        }
                    }
            ) {
                Text(
                    text = if (isNight)
                        "🌙 ${stringResource(R.string.night)}"
                    else
                        "☀️ ${stringResource(R.string.day)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

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
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Масштаб по FILL_CENTER
        val fillScale = max(
            canvasWidth / bitmapWidth.toFloat(),
            canvasHeight / bitmapHeight.toFloat()
        )

        val offsetX = (canvasWidth - bitmapWidth * fillScale) / 2f
        val offsetY = (canvasHeight - bitmapHeight * fillScale) / 2f

        val leftRect = rects.left
        val rightRect = rects.right

        // Координаты нижних углов фонарей
        val leftBottom = Offset(
            leftRect.x * fillScale + offsetX - 50f,
            (leftRect.y + leftRect.height) * fillScale + offsetY
        )
        val rightBottom = Offset(
            (rightRect.x + rightRect.width) * fillScale + offsetX + 50f,
            (rightRect.y + rightRect.height) * fillScale + offsetY
        )

        // Построение скругленного квадрата
        val squareWidth = rightBottom.x - leftBottom.x
        val squareHeight = squareWidth // Квадрат
        val cornerRadius = 100f

        val topLeft = Offset(leftBottom.x, leftBottom.y - squareHeight)
        val topRight = Offset(rightBottom.x, rightBottom.y - squareHeight)

        val path = Path().apply {
            moveTo(topLeft.x + cornerRadius, topLeft.y)

            // Верхняя сторона
            lineTo(topRight.x - cornerRadius, topRight.y)
            arcTo(
                Rect(
                    topRight.x - 2 * cornerRadius,
                    topRight.y,
                    topRight.x,
                    topRight.y + 2 * cornerRadius
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Правая сторона
            lineTo(rightBottom.x, rightBottom.y - cornerRadius)
            arcTo(
                Rect(
                    rightBottom.x - 2 * cornerRadius,
                    rightBottom.y - 2 * cornerRadius,
                    rightBottom.x,
                    rightBottom.y
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Нижняя сторона
            lineTo(leftBottom.x + cornerRadius, leftBottom.y)
            arcTo(
                Rect(
                    leftBottom.x,
                    leftBottom.y - 2 * cornerRadius,
                    leftBottom.x + 2 * cornerRadius,
                    leftBottom.y
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Левая сторона
            lineTo(topLeft.x, topLeft.y + cornerRadius)
            arcTo(
                Rect(
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

        // Рисуем рамки фонарей
        for (rect in listOf(leftRect, rightRect)) {
            val left = rect.x * fillScale + offsetX
            val top = rect.y * fillScale + offsetY
            val right = (rect.x + rect.width) * fillScale + offsetX
            val bottom = (rect.y + rect.height) * fillScale + offsetY

            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun AlarmTriggeredOverlay(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SlideToDismissButton(
                modifier = Modifier.padding(horizontal = 32.dp),
                onSlideComplete = onDismiss
            )
        }
    }
}

@Composable
fun SlideToDismissButton(
    modifier: Modifier = Modifier,
    onSlideComplete: () -> Unit
) {
    val swipeState = remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableIntStateOf(0) }
    val buttonSizePx = with(LocalDensity.current) { 64.dp.toPx() }
    val maxOffsetPx = (containerWidth - buttonSizePx).coerceAtLeast(0f)
    val threshold = 0.75f * maxOffsetPx

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .onGloballyPositioned { layoutCoordinates ->
                containerWidth = layoutCoordinates.size.width
            }
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeState.floatValue.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        swipeState.floatValue = (swipeState.floatValue + delta)
                            .coerceIn(0f, maxOffsetPx)
                    },
                    onDragStopped = {
                        if (swipeState.floatValue >= threshold) {
                            onSlideComplete()
                        }
                        swipeState.floatValue = 0f
                    }
                )
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White
            )
        }

        Text(
            text = stringResource(R.string.slide_to_dismiss),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
