package com.daristov.checkpoint.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

//TODO
@Composable
fun MotionFeedback(context: Context) {
    // Звук
    LaunchedEffect(Unit) {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
    }

    // Вибрация
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    // Визуальное мигание
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.3f))
    )
}
