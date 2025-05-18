package com.daristov.checkpoint.ui.components

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daristov.checkpoint.viewmodel.AlarmViewModel
import com.daristov.checkpoint.viewmodel.CalibrationStep

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview(viewModel: AlarmViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // ===== Настройка автоэкспозиции и автофокуса через Camera2Interop =====
                val previewBuilder = Preview.Builder()
                val camera2Ext = Camera2Interop.Extender(previewBuilder)

                camera2Ext.setCaptureRequestOption(
                    CaptureRequest.CONTROL_MODE,
                    CameraMetadata.CONTROL_MODE_AUTO
                )
                camera2Ext.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                camera2Ext.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO
                )
                camera2Ext.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )

                val preview = previewBuilder.build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // ===== Анализ изображений =====
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analyzer.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx),
                    ImageAnalysis.Analyzer { imageProxy ->
                        val bitmap = imageProxy.toBitmap()
                        if (bitmap != null) {
                            viewModel.onFrame(bitmap)
                        }
                        imageProxy.close()
                    }
                )

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analyzer
                )

                // Уведомим ViewModel, что камера готова
                viewModel.setStep(CalibrationStep.AUTO_ADJUSTING)
                Handler(Looper.getMainLooper()).postDelayed({
                    viewModel.setStep(CalibrationStep.SEARCHING_CONTOURS)
                }, 1500)

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

