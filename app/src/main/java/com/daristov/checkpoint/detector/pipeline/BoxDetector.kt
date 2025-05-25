package com.daristov.checkpoint.detector.pipeline

import com.daristov.checkpoint.domain.model.Line
import com.daristov.checkpoint.domain.model.QuadBox
import org.opencv.core.Point
import kotlin.math.abs

object BoxDetector {

    fun findTruckQuadFromLines(
        verticalLines: List<Line>,
        horizontalLines: List<Line>,
        cornerTolerance: Int = 100
    ): QuadBox? {
        val sortedVertical = verticalLines.sortedBy { it.centerX() }
        val candidates = mutableListOf<QuadBox>()

        for (i in sortedVertical.indices) {
            for (j in sortedVertical.lastIndex downTo i + 1) {
                val left = sortedVertical[i]
                val right = sortedVertical[j]

                if (right.centerX() - left.centerX() < 100) continue

                for (h in horizontalLines) {
                    val hY = h.centerY()
                    val leftBottom = if (left.y1 > left.y2) Point(left.x1.toDouble(),
                        left.y1.toDouble()
                    ) else Point(left.x2.toDouble(), left.y2.toDouble())
                    val rightBottom = if (right.y1 > right.y2) Point(right.x1.toDouble(),
                        right.y1.toDouble()
                    ) else Point(right.x2.toDouble(), right.y2.toDouble())

                    val hX1 = minOf(h.x1, h.x2)
                    val hX2 = maxOf(h.x1, h.x2)
                    val x1 = minOf(leftBottom.x, rightBottom.x)
                    val x2 = maxOf(leftBottom.x, rightBottom.x)

                    val xMatch = abs(hX1 - x1) < cornerTolerance && abs(hX2 - x2) < cornerTolerance
                    val yMatch = abs(hY - leftBottom.y) < cornerTolerance && abs(hY - rightBottom.y) < cornerTolerance

                    if (xMatch && yMatch) {
                        val leftTop = if (left.y1 < left.y2) Point(left.x1.toDouble(),
                            left.y1.toDouble()
                        ) else Point(left.x2.toDouble(), left.y2.toDouble())
                        val rightTop = if (right.y1 < right.y2) Point(right.x1.toDouble(),
                            right.y1.toDouble()
                        ) else Point(right.x2.toDouble(), right.y2.toDouble())

                        val box = QuadBox(
                            topLeft = leftTop,
                            topRight = rightTop,
                            bottomRight = rightBottom,
                            bottomLeft = leftBottom
                        )
                        candidates.add(box)
                    }
                }
            }
        }
        return candidates.maxByOrNull { boxArea(it) }
    }

    fun boxArea(box: QuadBox): Double {
        val width = box.topRight.x - box.topLeft.x
        val height = box.bottomLeft.y - box.topLeft.y
        return abs(width * height)
    }
}