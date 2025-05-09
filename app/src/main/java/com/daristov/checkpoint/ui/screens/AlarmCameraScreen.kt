package com.daristov.checkpoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.daristov.checkpoint.ui.components.CameraPreview
import com.daristov.checkpoint.viewmodel.AlarmViewModel
import com.daristov.checkpoint.viewmodel.CalibrationStep

@Composable
fun AlarmCameraScreen(
    navController: NavHostController,
    viewModel: AlarmViewModel = viewModel()
) {
    val calibrationStep by viewModel.calibrationStep.collectAsState()
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Камера — на заднем плане
        CameraPreview(viewModel = viewModel)

        // Нижняя панель поверх камеры с прозрачностью
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(160.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = Color.Transparent, // фон берётся из background выше
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = when (calibrationStep) {
                        CalibrationStep.WAITING_FOR_CAMERA -> "Подключаем камеру..."
                        CalibrationStep.AUTO_ADJUSTING -> "Автонастройка камеры под освещение..."
                        CalibrationStep.SEARCHING_CONTOURS -> "Анализ изображения, поиск контуров..."
                        CalibrationStep.WAITING_USER_CONFIRMATION -> "Подтвердите найденную область. Если не совпадает — укажите вручную."
                        CalibrationStep.TRACKING -> "Будильник включён. Следим за объектом..."
                        CalibrationStep.TRIGGERED -> "🚨 Обнаружено движение! Сигнал активирован."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { viewModel.recalibrate() }) {
                        Text("Перекалибровать")
                    }

                    Button(onClick = { navController.navigate("map") }) {
                        Text("Назад")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Будильник: ${if (state.motionDetected) "🚨 Сработал" else "🟢 Ожидает"}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
