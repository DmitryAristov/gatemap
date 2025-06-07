package com.daristov.checkpoint.detector

import com.daristov.checkpoint.detector.RearLightsDetector.RearLightPair
import org.opencv.core.Rect

class RearLightsMotionDetector(
    private var sensitivityThreshold: Double
) {
    private val history = ArrayDeque<DetectionEntry>()
    private val baseMinRisePx: Float = 100f
    private val baseMinShrinkPercent: Float = 0.2f
    private val timeWindowSec: Int = 4

    fun setSensitivity(value: Double) {
        sensitivityThreshold = value
    }

    fun update(current: RearLightPair): Boolean {
        val now = System.currentTimeMillis()
        history.addLast(DetectionEntry(now, current))

        val cutoff = now - timeWindowSec * 1000
        while (history.isNotEmpty() && history.first().timestamp < cutoff) {
            history.removeFirst()
        }

        val reference = history.firstOrNull() ?: return false

        val initialY = (reference.pair.left.centerY() + reference.pair.right.centerY()) / 2
        val currentY = (current.left.centerY() + current.right.centerY()) / 2
        val deltaY = initialY - currentY

        val initialWidth = reference.pair.left.width + reference.pair.right.width
        val currentWidth = current.left.width + current.right.width
        val shrink = 1.0 - (currentWidth.toFloat() / initialWidth.toFloat())

        val dynamicRise = baseMinRisePx * (sensitivityThreshold).toFloat()
        val dynamicShrink = baseMinShrinkPercent * (sensitivityThreshold).toFloat()

        return deltaY > dynamicRise || shrink > dynamicShrink
    }

    private data class DetectionEntry(val timestamp: Long, val pair: RearLightPair)
    private fun Rect.centerY(): Float = (this.y + this.height / 2f)
}
