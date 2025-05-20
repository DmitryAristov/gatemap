package com.daristov.checkpoint.detector.pipeline

import com.daristov.checkpoint.domain.model.Line
import com.daristov.checkpoint.domain.model.QuadBox
import com.daristov.checkpoint.detector.pipeline.BoxDetector.findTruckQuadFromLines
import com.daristov.checkpoint.detector.pipeline.LineMerger.mergeHorizontalLinesByRectangleStrict
import com.daristov.checkpoint.detector.pipeline.LineMerger.mergeVerticalLinesByRectangleStrict
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4
import kotlin.collections.map
import kotlin.math.abs

object VehicleContourDetector {

    fun findVehicleContourByLines(
        inputMat: Mat,
        blurSize: Int = 7,
        cannyThreshold1: Double = 100.0,
        cannyThreshold2: Double = 200.0
    ): QuadBox? {
        require(blurSize % 2 == 1) { "blurSize must be odd (e.g. 3, 5, 7)" }

        // Step 1: Grayscale
        val gray = Mat()
        Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY)

        // Step 2: Blur
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(blurSize.toDouble(), blurSize.toDouble()), 0.0)

        // Step 3: Canny
        val canny = Mat()
        Imgproc.Canny(blurred, canny, cannyThreshold1, cannyThreshold2)

        // Step 4: Dilate
        val dilated = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.dilate(canny, dilated, kernel, Point(-1.0, -1.0), 1)

        // Step 5: Morphological Closing
        val morph = Mat()
        val morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(dilated, morph, Imgproc.MORPH_CLOSE, morphKernel)

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

        if (vertical.size < 2 || horizontal.size < 2) {
            println("Недостаточно линий для формирования прямоугольника")
            return null
        }
        val minLineLength = 50
        val verticalFiltered = vertical.filter { abs(it.y2 - it.y1) > minLineLength }
        val horizontalFiltered = horizontal.filter { abs(it.x2 - it.x1) > minLineLength }

        val verticalMerged = mergeVerticalLinesByRectangleStrict(verticalFiltered)
        val horizontalMerged = mergeHorizontalLinesByRectangleStrict(horizontalFiltered)

        val quadBox = findTruckQuadFromLines(verticalMerged, horizontalMerged)
        if (quadBox != null) {
            return quadBox
        }
        return null
    }
}