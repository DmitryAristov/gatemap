package com.daristov.checkpoint.ui.components

import com.daristov.checkpoint.domain.model.QuadBox
import com.daristov.checkpoint.detector.pipeline.VehicleContourDetector
import org.junit.Test
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.core.Scalar
import java.io.File
import org.junit.Assert.assertFalse

class ContourDetectorTest {

    init {
        // Загрузка OpenCV native lib (должна быть подключена opencv_java455.dll/.so)
        nu.pattern.OpenCV.loadLocally()
    }

    @Test
    fun testContoursFromFileMat() {
        val inputMat = Imgcodecs.imread("src/test/resources/sample2.jpg")
        assertFalse("Изображение не загружено", inputMat.empty())
        val debugOutputDir: File = File("src/test/resources")

        val quadBox = VehicleContourDetector.findVehicleContourByLines(inputMat, blurSize = 5, cannyThreshold1 = 40.0, cannyThreshold2 = 120.0);
        if (quadBox != null) {
            val imageWithBox = drawQuadBox(inputMat, quadBox)
            Imgcodecs.imwrite(File(debugOutputDir, "step9_final_result.png").absolutePath, imageWithBox)
        }
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
}
