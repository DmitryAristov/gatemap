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

        val contour = findVehicleContourByLines(inputMat, blurSize = 5, cannyThreshold1 = 40.0, cannyThreshold2 = 120.0)
        println("contour size: " + contour?.size())

        // Нарисуем
        if (contour != null) {
            Imgproc.rectangle(inputMat, contour, Scalar(0.0, 255.0, 0.0), 3)
            Imgcodecs.imwrite("output_with_box.png", inputMat)
            Imgcodecs.imwrite(File(File("src/test/resources"), "output_with_box.png").absolutePath, inputMat)
        } else {
            println("Контур не найден.")
        }
        println("Saved to src/test/resources/output_with_box.png")
    }

    fun findVehicleContourByLines(
        inputMat: Mat,
        blurSize: Int = 7,
        cannyThreshold1: Double = 100.0,
        cannyThreshold2: Double = 200.0,
        debugOutputDir: File = File("src/test/resources")
    ): Rect? {
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
            return null
        }
        val minLineLength = 50
        val verticalFiltered = vertical.filter { abs(it.y2 - it.y1) > minLineLength }
        val horizontalFiltered = horizontal.filter { abs(it.x2 - it.x1) > minLineLength }

        val verticalMerged = mergeLines(verticalFiltered)
        val horizontalMerged = mergeLines(horizontalFiltered)

        drawMergedLines(
            morph.size(),
            verticalMerged,
            horizontalMerged,
            File(debugOutputDir, "step7_merged_all_lines.png")
        )

        val xPositions = verticalMerged.map { minOf(it.x1, it.x2) }
        val yPositions = horizontalMerged.map { minOf(it.y1, it.y2) }

        val (xMin, xMax) = getBoundaries(xPositions) ?: return null
        val (yMin, yMax) = getBoundaries(yPositions) ?: return null

        val boundingRect = Rect(Point(xMin.toDouble(), yMin.toDouble()), Point(xMax.toDouble(), yMax.toDouble()))

        // Step 8: Отрисовка финального прямоугольника
        val resultWithBox = inputMat.clone()
        Imgproc.rectangle(resultWithBox, boundingRect, Scalar(0.0, 255.0, 255.0), 3)
        Imgcodecs.imwrite(File(debugOutputDir, "step8_final_result.png").absolutePath, resultWithBox)

        return boundingRect
    }

    fun mergeLines(
        lines: List<Line>,
        angleTolerance: Double = 10.0,
        distanceThreshold: Int = 30
    ): List<Line> {
        val merged = mutableListOf<Line>()
        val used = BooleanArray(lines.size)

        for (i in lines.indices) {
            if (used[i]) continue
            val base = lines[i]
            val group = mutableListOf(base)
            used[i] = true

            for (j in i + 1 until lines.size) {
                if (used[j]) continue
                val candidate = lines[j]

                val similar = abs(base.angle() - candidate.angle()) < angleTolerance &&
                        distanceBetweenLines(base, candidate) < distanceThreshold

                if (similar) {
                    group.add(candidate)
                    used[j] = true
                }
            }

            // Средняя точка и угол
            val allPoints = group.flatMap { listOf(Point(it.x1.toDouble(), it.y1.toDouble()), Point(it.x2.toDouble(), it.y2.toDouble())) }
            val avgX = allPoints.map { it.x }.average()
            val avgY = allPoints.map { it.y }.average()
            val avgAngle = group.map { it.angle() }.average()

            // Общая длина по охватывающей прямоугольной рамке
            val minX = allPoints.minOf { it.x }
            val maxX = allPoints.maxOf { it.x }
            val minY = allPoints.minOf { it.y }
            val maxY = allPoints.maxOf { it.y }

            val lineLength = hypot(maxX - minX, maxY - minY)

            // Строим новую линию от центра в обе стороны
            val angleRad = Math.toRadians(avgAngle)
            val dx = cos(angleRad) * lineLength / 2.0
            val dy = sin(angleRad) * lineLength / 2.0

            val x1 = (avgX - dx).toInt()
            val y1 = (avgY - dy).toInt()
            val x2 = (avgX + dx).toInt()
            val y2 = (avgY + dy).toInt()

            merged.add(Line(x1, y1, x2, y2))
        }

        return merged
    }

    fun Line.angle(): Double = Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()))

    fun distanceBetweenLines(a: Line, b: Line): Double {
        val acx = (a.x1 + a.x2) / 2.0
        val acy = (a.y1 + a.y2) / 2.0
        val bcx = (b.x1 + b.x2) / 2.0
        val bcy = (b.y1 + b.y2) / 2.0
        return hypot(acx - bcx, acy - bcy)
    }


    fun Line.centerX() = (x1 + x2) / 2
    fun Line.centerY() = (y1 + y2) / 2

    fun getBoundaries(values: List<Int>, margin: Double = 0.1): Pair<Int, Int>? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val from = (sorted.size * margin).toInt()
        val to = (sorted.size * (1.0 - margin)).toInt()
        return sorted[from] to sorted[to - 1]
    }

    data class Line(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

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
