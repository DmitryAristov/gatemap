package com.daristov.checkpoint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.daristov.checkpoint.domain.model.QuadBox

@Composable
fun DrawQuadOverlay(
    box: QuadBox,
    color: Color = Color.Cyan,
    strokeWidth: Float = 4f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(box.topLeft.x.toFloat(), box.topLeft.y.toFloat())
            lineTo(box.topRight.x.toFloat(), box.topRight.y.toFloat())
            lineTo(box.bottomRight.x.toFloat(), box.bottomRight.y.toFloat())
            lineTo(box.bottomLeft.x.toFloat(), box.bottomLeft.y.toFloat())
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
}
