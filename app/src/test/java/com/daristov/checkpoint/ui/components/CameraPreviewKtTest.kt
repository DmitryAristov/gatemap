package com.daristov.checkpoint.ui.components

import org.junit.Test
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import java.io.File
import org.junit.Assert.assertFalse
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class ContourDetectorTest {

    init {
        // Загрузка OpenCV native lib (должна быть подключена opencv_java455.dll/.so)
        nu.pattern.OpenCV.loadLocally()
    }

    @Test
    fun testContoursFromFileMat() {
        val inputMat = Imgcodecs.imread("src/test/resources/sample2.jpg")
        assertFalse("Изображение не загружено", inputMat.empty())

        findVehicleContourByLines(inputMat, blurSize = 5, cannyThreshold1 = 40.0, cannyThreshold2 = 120.0)
    }

    fun findVehicleContourByLines(
        inputMat: Mat,
        blurSize: Int = 7,
        cannyThreshold1: Double = 100.0,
        cannyThreshold2: Double = 200.0,
        debugOutputDir: File = File("src/test/resources")
    ) {
        require(blurSize % 2 == 1) { "blurSize must be odd (e.g. 3, 5, 7)" }

        if (!debugOutputDir.exists()) debugOutputDir.mkdirs()

        // Step 1: Grayscale
        val gray = Mat()
        Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY)
        Imgcodecs.imwrite(File(debugOutputDir, "step1_gray.png").absolutePath, gray)

        // Step 2: Blur
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(blurSize.toDouble(), blurSize.toDouble()), 0.0)
        Imgcodecs.imwrite(File(debugOutputDir, "step2_blur.png").absolutePath, blurred)

        // Step 3: Canny
        val canny = Mat()
        Imgproc.Canny(blurred, canny, cannyThreshold1, cannyThreshold2)
        Imgcodecs.imwrite(File(debugOutputDir, "step3_canny.png").absolutePath, canny)

        // Step 4: Dilate
        val dilated = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.dilate(canny, dilated, kernel, Point(-1.0, -1.0), 1)
        Imgcodecs.imwrite(File(debugOutputDir, "step4_dilated.png").absolutePath, dilated)

        // Step 5: Morphological Closing
        val morph = Mat()
        val morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(dilated, morph, Imgproc.MORPH_CLOSE, morphKernel)
        Imgcodecs.imwrite(File(debugOutputDir, "step5_morph.png").absolutePath, morph)

        // Step 6: Hough Transform (на всём изображении)
        val lines = Mat()
        Imgproc.HoughLinesP(morph, lines, 1.0, Math.PI / 180, 50, 50.0, 10.0)

        val vertical = mutableListOf<Line>()
        val horizontal = mutableListOf<Line>()

        val debugLinesImage = Mat.zeros(morph.size(), CvType.CV_8UC3)

        for (i in 0 until lines.rows()) {
            val l = lines.get(i, 0)
            val (x1, y1, x2, y2) = l.map { it.toInt() }
            val angle = Math.abs(Math.toDegrees(kotlin.math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())))

            val color = if (angle in 80.0..100.0) Scalar(0.0, 255.0, 0.0) else Scalar(255.0, 0.0, 0.0)
            Imgproc.line(debugLinesImage, Point(x1.toDouble(), y1.toDouble()), Point(x2.toDouble(), y2.toDouble()), color, 2)

            if (angle in 80.0..100.0) vertical.add(Line(x1, y1, x2, y2))
            else if (angle in 0.0..10.0 || angle in 170.0..180.0) horizontal.add(Line(x1, y1, x2, y2))
        }

        Imgcodecs.imwrite(File(debugOutputDir, "step6_hough_lines.png").absolutePath, debugLinesImage)

        if (vertical.size < 2 || horizontal.size < 2) {
            println("Недостаточно линий для формирования прямоугольника")
            return
        }
        val minLineLength = 50
        val verticalFiltered = vertical.filter { abs(it.y2 - it.y1) > minLineLength }
        val horizontalFiltered = horizontal.filter { abs(it.x2 - it.x1) > minLineLength }

//        val verticalMerged = mergeLines(verticalFiltered)
//        val horizontalMerged = mergeLines(horizontalFiltered)
        val verticalMerged = mergeVerticalLinesByRectangleStrict(verticalFiltered)
        val horizontalMerged = mergeHorizontalLinesByRectangleStrict(horizontalFiltered)

        drawMergedLines(
            morph.size(),
            verticalMerged,
            horizontalMerged,
            File(debugOutputDir, "step7_merged_all_lines.png")
        )

        val quadBox = findTruckQuadFromLines(verticalMerged, horizontalMerged)
        if (quadBox != null) {
            val imageWithBox = drawQuadBox(inputMat, quadBox)
            Imgcodecs.imwrite(File(debugOutputDir, "step8_final_result.png").absolutePath, imageWithBox)
        }
    }

    fun findTruckQuadFromLines(
        verticalLines: List<Line>,
        horizontalLines: List<Line>,
        cornerTolerance: Int = 50
    ): QuadBox? {
        val sortedVertical = verticalLines.sortedBy { it.centerX() }

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

                        return QuadBox(
                            topLeft = leftTop,
                            topRight = rightTop,
                            bottomRight = rightBottom,
                            bottomLeft = leftBottom
                        )
                    }
                }
            }
        }

        return null
    }

    fun drawQuadBox(image: Mat, box: QuadBox, color: Scalar = Scalar(0.0, 255.0, 255.0), thickness: Int = 4): Mat {
        val output = image.clone()

        val points = listOf(
            box.topLeft,
            box.topRight,
            box.bottomRight,
            box.bottomLeft
        )

        for (i in points.indices) {
            val pt1 = points[i]
            val pt2 = points[(i + 1) % points.size]
            Imgproc.line(output, pt1, pt2, color, thickness)
        }

        return output
    }


    data class QuadBox(val topLeft: Point, val topRight: Point, val bottomRight: Point, val bottomLeft: Point)

    fun mergeVerticalLinesByRectangleStrict(
        lines: List<Line>,
        xTolerance: Int = 20,
        extendY: Int = 500
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
        yTolerance: Int = 20,
        extendX: Int = 500
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

    fun getBoundaries(values: List<Int>, margin: Double = 0.18): Pair<Int, Int>? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val from = (sorted.size * margin).toInt()
        val to = (sorted.size * (1.0 - margin)).toInt()
        return sorted[from] to sorted[to - 1]
    }

    data class Line(val x1: Int, val y1: Int, val x2: Int, val y2: Int) {
        fun centerX() = (x1 + x2) / 2
        fun centerY() = (y1 + y2) / 2
        fun minY(): Int = min(y1, y2)
        fun maxY(): Int = max(y1, y2)
    }

    fun drawMergedLines(
        size: Size,
        verticalLines: List<Line>,
        horizontalLines: List<Line>,
        outputFile: File
    ) {
        val image = Mat.zeros(size, CvType.CV_8UC3)

        // Вертикальные — жёлтые
        for (line in verticalLines) {
            Imgproc.line(
                image,
                Point(line.x1.toDouble(), line.y1.toDouble()),
                Point(line.x2.toDouble(), line.y2.toDouble()),
                Scalar(0.0, 255.0, 255.0), // Yellow
                2
            )
        }

        // Горизонтальные — голубые
        for (line in horizontalLines) {
            Imgproc.line(
                image,
                Point(line.x1.toDouble(), line.y1.toDouble()),
                Point(line.x2.toDouble(), line.y2.toDouble()),
                Scalar(255.0, 255.0, 0.0), // Cyan
                2
            )
        }

        Imgcodecs.imwrite(outputFile.absolutePath, image)
    }
}
