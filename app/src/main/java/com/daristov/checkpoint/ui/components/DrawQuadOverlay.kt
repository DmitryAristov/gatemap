package com.daristov.checkpoint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.daristov.checkpoint.domain.model.QuadBox
import org.opencv.core.Point

@Composable
fun DrawQuadOverlay(
    box: QuadBox,
    bitmapWidth: Int,
    bitmapHeight: Int,
    color: Color = Color.Red,
    strokeWidth: Float = 16f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Определяем масштаб с сохранением пропорций
        val scale = minOf(size.width / bitmapWidth, size.height / bitmapHeight)

        // Размер отмасштабированного изображения
        val scaledWidth = bitmapWidth * scale
        val scaledHeight = bitmapHeight * scale

        // Смещение, чтобы отрисовать по центру
        val offsetX = (size.width - scaledWidth) / 2f
        val offsetY = (size.height - scaledHeight) / 2f

        fun scalePoint(p: Point): Offset {
            return Offset(
                (p.x * scale).toFloat() + offsetX,
                (p.y * scale).toFloat() + offsetY
            )
        }

        val topLeft = scalePoint(box.topLeft)
        val topRight = scalePoint(box.topRight)
        val bottomRight = scalePoint(box.bottomRight)
        val bottomLeft = scalePoint(box.bottomLeft)

        val path = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
}
