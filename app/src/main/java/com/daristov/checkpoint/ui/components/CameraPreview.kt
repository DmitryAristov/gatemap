package com.daristov.checkpoint.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daristov.checkpoint.screens.alarm.AlarmViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    viewModel: AlarmViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = FIT_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
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
