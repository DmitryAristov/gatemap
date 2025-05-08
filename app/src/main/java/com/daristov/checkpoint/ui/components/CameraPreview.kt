package com.daristov.checkpoint.ui.components

import android.hardware.camera2.CaptureRequest
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daristov.checkpoint.viewmodel.AlarmViewModel

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview(viewModel: AlarmViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // === Camera2Interop: точная настройка ===
                val previewBuilder = Preview.Builder()
                val analysisBuilder = ImageAnalysis.Builder()

                val camera2ExtPreview = Camera2Interop.Extender(previewBuilder)
                val camera2ExtAnalysis = Camera2Interop.Extender(analysisBuilder)

                camera2ExtPreview.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                camera2ExtPreview.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO
                )
                camera2ExtPreview.setCaptureRequestOption(
                    CaptureRequest.CONTROL_MODE,
                    CaptureRequest.CONTROL_MODE_AUTO
                )
                // Можно добавить более глубокие параметры: ISO, эффект и т.п.

                val preview = previewBuilder.build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = analysisBuilder
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx),
                            ImageAnalysis.Analyzer { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                if (bitmap != null) {
                                    if (viewModel.state.value.referenceBitmap == null) {
                                        viewModel.setReference(bitmap)
                                    } else {
                                        viewModel.processFrame(bitmap)
                                    }
                                }
                                imageProxy.close()
                            }
                        )
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
