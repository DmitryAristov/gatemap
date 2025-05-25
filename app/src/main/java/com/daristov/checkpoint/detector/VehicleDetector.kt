package com.daristov.checkpoint.detector

import android.graphics.Bitmap
import com.daristov.checkpoint.domain.model.QuadBox
import com.daristov.checkpoint.detector.pipeline.VehicleContourDetector.findVehicleContourByLines
import com.daristov.checkpoint.domain.model.Line
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class VehicleDetector {

    fun process(bitmap: Bitmap): QuadBox? {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        return findVehicleContourByLines(mat)
    }
}
