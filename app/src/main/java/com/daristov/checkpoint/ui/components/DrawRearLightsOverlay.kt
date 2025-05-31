package com.daristov.checkpoint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import org.opencv.core.Rect

@Composable
fun DrawRearLightsOverlay(
    rects: List<Rect>,
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

        for (rect in rects) {
            val left = rect.x * scale + offsetX
            val top = rect.y * scale + offsetY
            val right = (rect.x + rect.width) * scale + offsetX
            val bottom = (rect.y + rect.height) * scale + offsetY

            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(left.toFloat(), top.toFloat()),
                size = androidx.compose.ui.geometry.Size(
                    (right - left).toFloat(),
                    (bottom - top).toFloat()
                ),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
