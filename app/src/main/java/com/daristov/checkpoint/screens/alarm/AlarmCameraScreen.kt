package com.daristov.checkpoint.screens.alarm

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.daristov.checkpoint.ui.components.CameraPreview
import com.daristov.checkpoint.ui.components.DrawRearLightsOverlay
import com.daristov.checkpoint.screens.settings.SettingsPreferenceManager

@Composable
fun AlarmCameraScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val settingsManager = remember { SettingsPreferenceManager(context) }
    val factory = remember { AlarmViewModelFactory(application, settingsManager) }
    val viewModel: AlarmViewModel = viewModel(factory = factory)

    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Камера с отступом под статус-бар
        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .padding(WindowInsets.statusBars.asPaddingValues())
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
                .defaultMinSize(minHeight = 200.dp)
                .background(Color.Black)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.motionDetected) "🚨 Обнаружено движение!" else "🟢 Ожидает",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (state.isNight == true) "🌙 Ночь" else "☀️ День",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )
                Button(onClick = { navController.navigate("map") }) {
                    Text("Назад")
                }
            }
        }
    }
}
