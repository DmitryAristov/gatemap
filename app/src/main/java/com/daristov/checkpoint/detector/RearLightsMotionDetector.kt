package com.daristov.checkpoint.detector

import com.daristov.checkpoint.detector.RearLightsDetector.RearLightPair
import org.opencv.core.Rect

class RearLightsMotionDetector(
    private val historySize: Int = 20,
    private val minMoveFrames: Int = 3,
    private val minShrinkPercent: Double = 0.1, // 10%
    private val minRisePx: Int = 20
) {
    private val history = ArrayDeque<RearLightPair>()

    fun update(newDetection: RearLightPair): Boolean {
        history.addLast(newDetection)
        if (history.size > historySize) {
            history.removeFirst()
        }

        // Проверяем последние кадры
        if (history.size < minMoveFrames + 1) return false

        val first = history.first()
        val last = history.last()

        // 1. Уменьшение размера (ширина фонарей)
        val initialWidth = first.left.width + first.right.width
        val currentWidth = last.left.width + last.right.width
        val shrinkPercent = 1.0 - (currentWidth.toDouble() / initialWidth)

        // 2. Смещение вверх
        val initialCenterY = (first.left.centerY() + first.right.centerY()) / 2
        val currentCenterY = (last.left.centerY() + last.right.centerY()) / 2
        val rise = initialCenterY - currentCenterY

        return shrinkPercent > minShrinkPercent || rise > minRisePx
    }

    private fun Rect.centerY(): Float = (this.y + this.height / 2f)
}
