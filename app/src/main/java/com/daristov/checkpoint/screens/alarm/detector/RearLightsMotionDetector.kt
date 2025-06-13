package com.daristov.checkpoint.screens.alarm.detector

import android.util.Log
import com.daristov.checkpoint.screens.alarm.detector.RearLightsDetector.RearLightPair
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.RotatedRect
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

private const val TIME_WINDOW_SECONDS: Int = 4
private const val MAX_POINT_SEPARATION_RATIO: Float = 0.002f          // 0.2% от размеров кадра
private const val MAX_LINE_DEVIATION_RATIO: Float = 0.03f             // 2% от размеров кадра
private const val MIN_VERTICAL_MOVEMENT_RATIO: Float = 0.2f           // 20% от высоты кадра
private const val MIN_HORIZONTAL_COMPRESSION_RATIO: Float = 0.2f      // 20% от ширины кадра

class RearLightsMotionDetector(width: Int, height: Int, vertical: Double, horizontal: Double, stable: Double) {

    private val TAG: String = "RearLightsMotionDetector"

    private val MAX_POINT_SEPARATION_PX: Float
    private val MAX_LINE_DEVIATION_PX: Float
    private val MIN_VERTICAL_MOVEMENT_PX: Float
    private val MIN_HORIZONTAL_COMPRESSION_PX: Float

    private val verticalMovementSensitivity: Double = vertical
    private val horizontalCompressionSensitivity: Double = horizontal
    private val stableTrajectoryRatio: Double = stable

    init {
        val frameAverage = (width + height).toFloat() / 2f
        MAX_POINT_SEPARATION_PX = frameAverage * MAX_POINT_SEPARATION_RATIO
        MAX_LINE_DEVIATION_PX = frameAverage * MAX_LINE_DEVIATION_RATIO
        MIN_VERTICAL_MOVEMENT_PX = height * MIN_VERTICAL_MOVEMENT_RATIO
        MIN_HORIZONTAL_COMPRESSION_PX = width * MIN_HORIZONTAL_COMPRESSION_RATIO
        Log.i(TAG, "Initialize motion detector with values: \n" +
                "   width: $width\n" +
                "   height: $height\n" +
                "   vertical: $vertical\n" +
                "   horizontal: $horizontal\n" +
                "   stable: $stable")
    }

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
        return checkUpMotion(leftStart, leftEnd, rightStart, rightEnd)
    }

    private fun filterStableTrajectory(points: List<PointF>): List<PointF> {
        if (points.size < 2) return emptyList()

        val stable = mutableListOf<PointF>()
        stable.add(points[0])

        for (i in 1 until points.size) {
            val d = distance(points[i], points[i - 1])
            if (d <= MAX_POINT_SEPARATION_PX) {
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

        return shortSide <= MAX_LINE_DEVIATION_PX
    }

    private fun checkUpMotion(leftStart: PointF, leftEnd: PointF, rightStart: PointF, rightEnd: PointF): Boolean {
        val verticalMovementLeftDelta = leftStart.y - leftEnd.y
        val verticalMovementRightDelta = rightStart.y - rightEnd.y
        val finalVerticalMovementThreshold = MIN_VERTICAL_MOVEMENT_PX * verticalMovementSensitivity
        if (verticalMovementLeftDelta > finalVerticalMovementThreshold && verticalMovementRightDelta > finalVerticalMovementThreshold) {
            Log.i(TAG, "Found vertical motion {left: $verticalMovementLeftDelta, right: $verticalMovementRightDelta, final: $finalVerticalMovementThreshold}")
            return true
        }
        return false
    }

    private fun checkDistanceDecrease(leftStart: PointF, rightStart: PointF, leftEnd: PointF, rightEnd: PointF): Boolean {
        val initialDistance = distance(leftStart, rightStart)
        val finalDistance = distance(leftEnd, rightEnd)
        val horizontalDelta = initialDistance - finalDistance
        val finalHorizontalThreshold = MIN_HORIZONTAL_COMPRESSION_PX * horizontalCompressionSensitivity

        if (horizontalDelta > finalHorizontalThreshold) {
            Log.i(TAG, "Found horizontal motion {delta: $horizontalDelta, final: $horizontalCompressionSensitivity}")
            return true
        }
        return false
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    private data class DetectionEntry(val timestamp: Long, val pair: RearLightPair)
    private data class PointF(val x: Float, val y: Float)

    private fun Rect.center(): PointF = PointF(
        this.x + this.width / 2f,
        this.y + this.height / 2f
    )
}
