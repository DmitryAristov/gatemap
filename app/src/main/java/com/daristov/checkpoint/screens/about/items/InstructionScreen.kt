package com.daristov.checkpoint.screens.about.items

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.daristov.checkpoint.R
import kotlinx.coroutines.delay

@Composable
fun InstructionScreen(navController: NavHostController) {
    val steps = listOf(
        InstructionStep("Шаг 1", "Откройте карту и найдите ближайший КПП", imageRes = 0),
        InstructionStep("Шаг 2", "Нажмите на значок, чтобы увидеть информацию", imageRes = 0),
        InstructionStep("Шаг 3", "Включите будильник, когда встали в очередь", imageRes = 0),
        InstructionStep("Шаг 4", "Настройте чувствительность в настройках", imageRes = /*R.drawable.instruction_step_4*/0),
    )

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler {
        navController.navigate("about")
    }

    LaunchedEffect(currentStepIndex) {
        progress.snapTo(0f)
    }

    LaunchedEffect(currentStepIndex, isPaused) {
        if (!isPaused) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000)
            )
            if (!isPaused) {
                if (currentStepIndex < steps.lastIndex) {
                    currentStepIndex++
                } else {
                    navController.navigate("about")
                }
            }
        }
    }

    Scaffold { padding ->
        // 🔒 Изображения временно отключены
        // Картинка на весь экран

//        Image(
//            painter = painterResource(id = steps[currentStep].imageRes),
//            contentDescription = null,
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.fillMaxSize()
//        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = {}
                    )
                }
        ) {
            val step = steps[currentStepIndex]

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentStepIndex) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val startX = down.position.x
                                val up = waitForUpOrCancellation()
                                val endX = up?.position?.x ?: startX
                                val delta = endX - startX
                                if (delta > 100 && currentStepIndex > 0) {
                                    currentStepIndex--
                                } else if (delta < -100 && currentStepIndex < steps.lastIndex) {
                                    currentStepIndex++
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

data class InstructionStep(val title: String, val description: String, val imageRes: Int)