package com.daristov.checkpoint.screens.alarm

import android.graphics.Bitmap

class ImageStabilityDetector {

    private var lastBitmapHash: Int? = null

    fun compareWithPrevious(bitmap: Bitmap): Boolean {
        val currentHash = computeImageHash(bitmap)
        val isStable = currentHash == lastBitmapHash
        lastBitmapHash = currentHash
        return isStable
    }

    private fun computeImageHash(bitmap: Bitmap): Int {
        // Быстрый способ: resize + grayscale + hash
        val resized = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        return resized.pixelHash()
    }

    private fun Bitmap.pixelHash(): Int {
        var result = 1
        for (x in 0 until width step 4) {
            for (y in 0 until height step 4) {
                result = 31 * result + getPixel(x, y)
            }
        }
        return result
    }
}
