package com.daristov.checkpoint.detector

import com.daristov.checkpoint.detector.RearLightsDetector.RearLightPair
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.RotatedRect
import org.opencv.imgproc.Imgproc

private const val MAX_STEP_DISTANCE: Float = 10f
private const val MAX_LINE_WIDTH_PX: Float = 20f
private const val TIME_WINDOW_SECONDS: Int = 3

class RearLightsMotionDetector() {

    private var stableTrajectoryRatio: Double = 0.8

    private var shrinkThreshold: Float = 100f
    private var shrinkSensitivity: Double = 0.7

    private var riseThreshold: Float = 100f
    private var riseSensitivity: Double = 0.7

    private val history = ArrayDeque<DetectionEntry>()

    fun update(current: RearLightPair): Boolean {
        val now = System.currentTimeMillis()
        history.addLast(DetectionEntry(now, current))

        val cutoff = now - TIME_WINDOW_SECONDS * 1000
        while (history.isNotEmpty() && history.first().timestamp < cutoff) {
            history.removeFirst()
        }
        return detectMotion(history.toList())
    }

    private fun detectMotion(entries: List<DetectionEntry>): Boolean {
        val size = entries.size

        if (size < 3)
            return false

        val sorted = entries.sortedBy { it.timestamp }

        val leftPoints = sorted.map { it.pair.left.center() }
        val rightPoints = sorted.map { it.pair.right.center() }

        // Анализ стабильности точек
        val stableLeftPoints = filterStableTrajectory(leftPoints)
        val stableRightPoints = filterStableTrajectory(rightPoints)

        if (stableLeftPoints.isEmpty() || stableRightPoints.isEmpty())
            return false

        // Анализ прямоты линии
        if (!isLineCompact(stableLeftPoints) || !isLineCompact(stableRightPoints))
            return false

        // Начальные и конечные точки
        val leftStart = leftPoints.first()
        val leftEnd = leftPoints.last()
        val rightStart = rightPoints.first()
        val rightEnd = rightPoints.last()

        // проверка минимального сокращения дистанции между сторонами
        if (checkDistanceDecrease(leftStart, rightStart, leftEnd, rightEnd))
            return true

        // проверка минимального смещения вверх
        return checkUpMoving(leftStart, leftEnd, rightStart, rightEnd)
    }

    private fun filterStableTrajectory(points: List<PointF>): List<PointF> {
        if (points.size < 2) return emptyList()

        val stable = mutableListOf<PointF>()
        stable.add(points[0])

        for (i in 1 until points.size) {
            val d = distance(points[i], points[i - 1])
            if (d <= MAX_STEP_DISTANCE) {
                stable.add(points[i])
            }
        }

        val ratio = stable.size.toFloat() / points.size
        return if (ratio >= stableTrajectoryRatio) stable else emptyList()
    }

    private fun isLineCompact(points: List<PointF>): Boolean {
        if (points.size < 2)
            return false

        val pointArray = points.map { Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray()
        val mat = MatOfPoint2f(*pointArray)

        val rotatedRect: RotatedRect = Imgproc.minAreaRect(mat)

        val width = rotatedRect.size.width
        val height = rotatedRect.size.height
        val shortSide = minOf(width, height)

        return shortSide <= MAX_LINE_WIDTH_PX
    }

    private fun checkUpMoving(leftStart: PointF, leftEnd: PointF, rightStart: PointF, rightEnd: PointF): Boolean {
        val shrinkLeftDelta = leftStart.y - leftEnd.y
        val shrinkRightDelta = rightStart.y - rightEnd.y
        val finalShrinkThreshold = shrinkThreshold * shrinkSensitivity
        return shrinkLeftDelta > finalShrinkThreshold && shrinkRightDelta > finalShrinkThreshold
    }

    private fun checkDistanceDecrease(leftStart: PointF, rightStart: PointF, leftEnd: PointF, rightEnd: PointF): Boolean {
        val initialDistance = distance(leftStart, rightStart)
        val finalDistance = distance(leftEnd, rightEnd)
        val riseDelta = initialDistance - finalDistance

        if (riseDelta > riseThreshold * riseSensitivity) return true
        return false
    }

    fun setRiseSensitivity(value: Double) {
        riseSensitivity = value
    }

    fun setShrinkSensitivity(value: Double) {
        shrinkSensitivity = value
    }

    fun setStableTrajectoryRatio(value: Double) {
        stableTrajectoryRatio = value
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private data class DetectionEntry(val timestamp: Long, val pair: RearLightPair)
    private data class PointF(val x: Float, val y: Float)

    private fun Rect.center(): PointF = PointF(
        this.x + this.width / 2f,
        this.y + this.height / 2f
    )
}
