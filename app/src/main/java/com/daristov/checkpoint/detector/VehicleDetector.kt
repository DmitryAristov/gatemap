package com.daristov.checkpoint.detector

import android.graphics.Bitmap
import com.daristov.checkpoint.detector.RearLightsDetector.RearLightPair
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import com.daristov.checkpoint.detector.RearLightsDetector.extractRedMask
import com.daristov.checkpoint.detector.RearLightsDetector.filterMask
import com.daristov.checkpoint.detector.RearLightsDetector.findContours
import com.daristov.checkpoint.detector.RearLightsDetector.filterRedLightCandidates

class VehicleDetector {

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
}
