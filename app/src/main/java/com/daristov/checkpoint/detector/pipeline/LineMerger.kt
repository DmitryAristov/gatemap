package com.daristov.checkpoint.detector.pipeline

import com.daristov.checkpoint.domain.model.Line
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

object LineMerger {

    fun mergeVerticalLinesByRectangleStrict(
        lines: List<Line>,
        xTolerance: Int = 10,
        extendY: Int = 1000
    ): List<Line> {
        val remaining = lines.toMutableList()
        val merged = mutableListOf<Line>()

        while (remaining.isNotEmpty()) {
            val base = remaining.removeAt(0)

            val baseX = base.centerX()
            var minY = base.minY() - extendY
            var maxY = base.maxY() + extendY

            val cluster = mutableListOf(base)

            var mergedSomething: Boolean
            do {
                mergedSomething = false
                val iterator = remaining.iterator()

                while (iterator.hasNext()) {
                    val other = iterator.next()
                    val x = other.centerX()
                    val y1 = other.minY()
                    val y2 = other.maxY()

                    val insideX = x in (baseX - xTolerance)..(baseX + xTolerance)
                    val insideY = y1 >= minY && y2 <= maxY

                    if (insideX && insideY) {
                        minY = min(minY, y1)
                        maxY = max(maxY, y2)
                        cluster.add(other)
                        iterator.remove()
                        mergedSomething = true
                    }
                }
            } while (mergedSomething)

            val points = cluster.flatMap {
                listOf(Point(it.x1.toDouble(), it.y1.toDouble()), Point(it.x2.toDouble(), it.y2.toDouble()))
            }

            merged.add(fitLineFromPoints(points))
        }

        return merged
    }

    fun mergeHorizontalLinesByRectangleStrict(
        lines: List<Line>,
        yTolerance: Int = 10,
        extendX: Int = 1000
    ): List<Line> {
        val remaining = lines.toMutableList()
        val merged = mutableListOf<Line>()

        while (remaining.isNotEmpty()) {
            val base = remaining.removeAt(0)

            val baseY = base.centerY()
            var minX = minOf(base.x1, base.x2) - extendX
            var maxX = maxOf(base.x1, base.x2) + extendX

            val cluster = mutableListOf(base)

            var mergedSomething: Boolean
            do {
                mergedSomething = false
                val iterator = remaining.iterator()

                while (iterator.hasNext()) {
                    val other = iterator.next()
                    val y = other.centerY()
                    val x1 = minOf(other.x1, other.x2)
                    val x2 = maxOf(other.x1, other.x2)

                    val insideY = y in (baseY - yTolerance)..(baseY + yTolerance)
                    val insideX = x1 >= minX && x2 <= maxX

                    if (insideY && insideX) {
                        minX = min(minX, x1)
                        maxX = max(maxX, x2)
                        cluster.add(other)
                        iterator.remove()
                        mergedSomething = true
                    }
                }
            } while (mergedSomething)

            val points = cluster.flatMap {
                listOf(Point(it.x1.toDouble(), it.y1.toDouble()), Point(it.x2.toDouble(), it.y2.toDouble()))
            }

            merged.add(fitLineFromPoints(points))
        }

        return merged
    }

    fun fitLineFromPoints(points: List<Point>): Line {
        val mat = MatOfPoint2f(*points.toTypedArray())
        val lineParams = Mat()
        Imgproc.fitLine(mat, lineParams, Imgproc.DIST_L2, 0.0, 0.01, 0.01)

        val vx = lineParams[0, 0]!![0]
        val vy = lineParams[1, 0]!![0]
        val x0 = lineParams[2, 0]!![0]
        val y0 = lineParams[3, 0]!![0]

        // проекции
        val projections = points.map { p ->
            val t = (vx * (p.x - x0) + vy * (p.y - y0)) / (vx * vx + vy * vy)
            Point(x0 + t * vx, y0 + t * vy)
        }

        val sorted = projections.sortedBy { it.x + it.y }
        val p1 = sorted.first()
        val p2 = sorted.last()

        return Line(p1.x.toInt(), p1.y.toInt(), p2.x.toInt(), p2.y.toInt())
    }
}