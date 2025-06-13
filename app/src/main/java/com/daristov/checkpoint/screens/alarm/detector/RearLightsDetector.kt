package com.daristov.checkpoint.screens.alarm.detector

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

private const val minArea = 200
private const val maxDeltaY = 50

class RearLightsDetector {

    fun process(bitmap: Bitmap): RearLightPair? {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        val redMask = extractRedMask(mat)
        val cleanedMask = filterMask(redMask)
        val contours = findContours(cleanedMask)
        val rearLightPair = filterRedLightCandidates(contours, mat.size())
        return rearLightPair
    }

    // Основная функция получения маски
    fun extractRedMask(input: Mat): Mat {
        val hsv = Mat()
        Imgproc.cvtColor(input, hsv, Imgproc.COLOR_BGR2HSV)
        val isNight = isNightImage(hsv)
        return if (isNight) extractBrightRedMask(hsv) else extractRedMaskDay(hsv)
    }

    private fun extractRedMaskDay(hsv: Mat): Mat {
        val lower1 = Scalar(0.0, 100.0, 100.0)
        val upper1 = Scalar(10.0, 255.0, 255.0)
        val lower2 = Scalar(160.0, 100.0, 100.0)
        val upper2 = Scalar(180.0, 255.0, 255.0)
        return combineRedMasks(hsv, lower1, upper1, lower2, upper2)
    }

    private fun extractBrightRedMask(hsv: Mat): Mat {
        val lower1 = Scalar(0.0, 50.0, 200.0)
        val upper1 = Scalar(10.0, 255.0, 255.0)
        val lower2 = Scalar(160.0, 50.0, 200.0)
        val upper2 = Scalar(180.0, 255.0, 255.0)
        val redMask = combineRedMasks(hsv, lower1, upper1, lower2, upper2)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(redMask, redMask, Imgproc.MORPH_CLOSE, kernel)

        return redMask
    }

    private fun combineRedMasks(hsv: Mat, lower1: Scalar, upper1: Scalar, lower2: Scalar, upper2: Scalar): Mat {
        val mask1 = Mat()
        val mask2 = Mat()
        Core.inRange(hsv, lower1, upper1, mask1)
        Core.inRange(hsv, lower2, upper2, mask2)
        val result = Mat()
        Core.add(mask1, mask2, result)
        return result
    }

    internal fun isNightImage(hsv: Mat): Boolean {
        val vChannel = ArrayList<Mat>()
        Core.split(hsv, vChannel)
        val mean = Core.mean(vChannel[2])
        return mean.`val`[0] < 100.0
    }

    fun filterMask(mask: Mat): Mat {
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        val morphed = Mat()
        Imgproc.morphologyEx(mask, morphed, Imgproc.MORPH_OPEN, kernel)
        Imgproc.morphologyEx(morphed, morphed, Imgproc.MORPH_CLOSE, kernel)
        return morphed
    }

    fun findContours(mask: Mat): List<MatOfPoint> {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        return contours
    }

    fun filterRedLightCandidates(contours: List<MatOfPoint>, imageSize: Size): RearLightPair? {
        val allRects = contours
            .map { Imgproc.boundingRect(it) }
            .filter { it.width * it.height > minArea }
            .sortedByDescending { it.y + it.height / 2 }

        for ((index, current) in allRects.withIndex()) {
            val centerX = current.x + current.width / 2
            val centerY = current.y + current.height / 2
            val isLeft = centerX < imageSize.width / 2

            for (other in allRects.subList(index + 1, allRects.size)) {
                val otherCenterX = other.x + other.width / 2
                val otherCenterY = other.y + other.height / 2
                val isOpposite = if (isLeft) otherCenterX > imageSize.width / 2 else otherCenterX < imageSize.width / 2

                if (isOpposite && abs(centerY - otherCenterY) <= maxDeltaY) {
                    return if (isLeft) RearLightPair(current, other) else RearLightPair(other, current)
                }
            }
        }
        return null
    }

    data class RearLightPair(val left: Rect, val right: Rect)
}
