package com.daristov.checkpoint.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.daristov.checkpoint.ui.components.CameraPreview
import com.daristov.checkpoint.ui.components.MotionFeedback
import com.daristov.checkpoint.viewmodel.AlarmViewModel

@Composable
fun AlarmCameraScreen_(navController: NavHostController) {
    Box(Modifier.fillMaxSize()) {
        Text("[Здесь будет превью камеры и логика OpenCV]", modifier = Modifier.align(Alignment.Center))
        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") //todo replacing
        }
    }
}

@Composable
fun AlarmCameraScreen(navController: NavHostController, viewModel: AlarmViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    Box(Modifier.fillMaxSize()) {
        CameraPreview(viewModel = viewModel)

        // Инструкция
        Text(
            text = "Направьте камеру на машину перед вами",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        // Панель управления
        Row(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            IconButton(onClick = { viewModel.recalibrate() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Перекалибровка")
            }
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Close, contentDescription = "Назад")
            }
        }

        // Индикатор состояния
        Text(
            text = if (state.motionDetected) "Движение!" else "Ожидание...",
            color = if (state.motionDetected) Color.Red else Color.Green,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            style = MaterialTheme.typography.titleMedium
        )

        if (state.motionDetected) {
            MotionFeedback(context)
        }
    }
}

