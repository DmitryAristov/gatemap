package com.daristov.checkpoint.detector

import android.graphics.Bitmap
import com.daristov.checkpoint.detector.RearLightsDetector
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

class VehicleDetector {

    fun process(bitmap: Bitmap): List<Rect> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        val redMask = RearLightsDetector.extractRedMask(mat)
        val cleanedMask = RearLightsDetector.filterMask(redMask)
        val contours = RearLightsDetector.findContours(cleanedMask)
        val candidates = RearLightsDetector.filterRedLightCandidates(contours, mat.size())
        return candidates
    }
}
