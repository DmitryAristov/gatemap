package com.daristov.checkpoint.detector.pipeline

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object RearLightsDetector {

    // 1. Перевод изображения в HSV и маска по красному цвету
    fun extractRedMask(input: Mat): Mat {
        val hsv = Mat()
        Imgproc.cvtColor(input, hsv, Imgproc.COLOR_BGR2HSV)

        val lowerRed1 = Scalar(0.0, 100.0, 100.0)
        val upperRed1 = Scalar(10.0, 255.0, 255.0)

        val lowerRed2 = Scalar(160.0, 100.0, 100.0)
        val upperRed2 = Scalar(180.0, 255.0, 255.0)

        val mask1 = Mat()
        val mask2 = Mat()

        Core.inRange(hsv, lowerRed1, upperRed1, mask1)
        Core.inRange(hsv, lowerRed2, upperRed2, mask2)

        val redMask = Mat()
        Core.add(mask1, mask2, redMask)

        return redMask
    }

    // 2. Морфологическая фильтрация для устранения шума
    fun filterMask(mask: Mat): Mat {
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        val morphed = Mat()
        Imgproc.morphologyEx(mask, morphed, Imgproc.MORPH_OPEN, kernel)
        Imgproc.morphologyEx(morphed, morphed, Imgproc.MORPH_CLOSE, kernel)
        return morphed
    }

    // 3. Поиск контуров на маске
    fun findContours(filteredMask: Mat): List<MatOfPoint> {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            filteredMask,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        return contours
    }

    fun filterRedLightCandidates(contours: List<MatOfPoint>, imageSize: Size): List<Rect> {
        val minArea = 500        // минимальная площадь прямоугольника
        val maxDeltaY = 50       // максимально допустимая разница по вертикали между фарами

        val allRects = contours
            .map { Imgproc.boundingRect(it) }
            .filter { it.width * it.height > minArea }
            .sortedByDescending { it.y + it.height / 2 } // снизу вверх

        for ((index, current) in allRects.withIndex()) {
            val centerX = current.x + current.width / 2
            val centerY = current.y + current.height / 2

            val isLeft = centerX < imageSize.width / 2

            val searchRange = allRects.subList(index + 1, allRects.size)

            for (other in searchRange) {
                val otherCenterX = other.x + other.width / 2
                val otherCenterY = other.y + other.height / 2

                val isOppositeSide = if (isLeft)
                    otherCenterX > imageSize.width / 2
                else
                    otherCenterX < imageSize.width / 2

                if (isOppositeSide && kotlin.math.abs(centerY - otherCenterY) <= maxDeltaY) {
                    return if (isLeft) listOf(current, other) else listOf(other, current)
                }
            }
        }

        return emptyList()
    }


    // 5. Визуализация результатов (опционально)
    fun drawCandidateRects(image: Mat, candidates: List<Rect>) {
        for (rect in candidates) {
            Imgproc.rectangle(image, rect.tl(), rect.br(), Scalar(0.0, 255.0, 0.0), 2)
        }
    }
}

