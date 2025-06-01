package com.daristov.checkpoint.ui.components

import com.daristov.checkpoint.detector.RearLightsDetector
import org.junit.Assert.assertFalse
import org.junit.Test
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

class RearLightsDetectorTest {

    val debugOutputDir = File("src/test/resources")
    val inputMatPath = "src/test/resources/sample3_night.jpg"
    init {
        nu.pattern.OpenCV.loadLocally()
    }

    @Test
    fun extractRedMaskTest() {
        val inputMat = Imgcodecs.imread(inputMatPath)
        assertFalse("Изображение не загружено", inputMat.empty())
        val redMask = RearLightsDetector.extractRedMask(inputMat)

        val result = Mat()
        Core.bitwise_and(inputMat, inputMat, result, redMask)

        Imgcodecs.imwrite(File(debugOutputDir, "sample2_0_extractRedMask.png").absolutePath, result)
    }

    @Test
    fun filterMaskTest() {
        val inputMat = Imgcodecs.imread(inputMatPath)
        assertFalse("Изображение не загружено", inputMat.empty())

        val redMask = RearLightsDetector.extractRedMask(inputMat)
        val cleanedMask = RearLightsDetector.filterMask(redMask)

        val result = Mat()
        Core.bitwise_and(inputMat, inputMat, result, cleanedMask)

        Imgcodecs.imwrite(File(debugOutputDir, "sample2_1_filterMask.png").absolutePath, result)
    }

    @Test
    fun findContoursTest() {
        val inputMat = Imgcodecs.imread(inputMatPath)
        assertFalse("Изображение не загружено", inputMat.empty())

        val redMask = RearLightsDetector.extractRedMask(inputMat)
        val cleanedMask = RearLightsDetector.filterMask(redMask)
        val contours = RearLightsDetector.findContours(cleanedMask)

        // Копия изображения для отрисовки
        val outputMat = inputMat.clone()
        Imgproc.drawContours(outputMat, contours, -1, Scalar(0.0, 255.0, 0.0), 2)

        // Сохраняем результат
        val outFile = File(debugOutputDir, "sample2_2_findContours.png")
        Imgcodecs.imwrite(outFile.absolutePath, outputMat)
    }

    @Test
    fun filterRedLightCandidatesTest() {
        val inputMat = Imgcodecs.imread(inputMatPath)
        assertFalse("Изображение не загружено", inputMat.empty())
        val redMask = RearLightsDetector.extractRedMask(inputMat)
        val cleanedMask = RearLightsDetector.filterMask(redMask)
        val contours = RearLightsDetector.findContours(cleanedMask)
        val rearLightPair = RearLightsDetector.filterRedLightCandidates(contours, inputMat.size())
        if (rearLightPair != null) {
            Imgproc.rectangle(inputMat, rearLightPair.left.tl(), rearLightPair.left.br(), Scalar(0.0, 255.0, 0.0), 6)
            Imgproc.rectangle(inputMat, rearLightPair.right.tl(), rearLightPair.right.br(), Scalar(0.0, 255.0, 0.0), 6)
        } else
            assertFalse("Не удалось найти фонари", true)

        Imgcodecs.imwrite(File(debugOutputDir, "sample2_3_filterRedLightCandidates.png").absolutePath, inputMat)
    }
}