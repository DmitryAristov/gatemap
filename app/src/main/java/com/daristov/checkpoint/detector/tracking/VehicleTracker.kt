package com.daristov.checkpoint.detector.tracking

import com.daristov.checkpoint.domain.model.QuadBox
import org.opencv.core.Point
import java.util.ArrayDeque
import kotlin.math.hypot

class VehicleTracker(
    private val maxHistorySize: Int = 8,
    private val matchTolerance: Float = 30f,
    private val requiredMatches: Int = 5
) {
    private val history = ArrayDeque<QuadBox>()

    var currentStableBox: QuadBox? = null
        private set

    fun update(newBox: QuadBox): Boolean {
        history.addLast(newBox)
        if (history.size > maxHistorySize && history.isNotEmpty())
            history.removeFirst()

        val mostFrequentBox = findMostFrequentBox()

        return if (mostFrequentBox != null) {
            currentStableBox = mostFrequentBox
            true
        } else {
            false
        }
    }

    private fun findMostFrequentBox(): QuadBox? {
        var bestBox: QuadBox? = null
        var bestCount = 0

        for (candidate in history) {
            val count = history.count { isSimilar(candidate, it) }
            if (count > bestCount && count >= requiredMatches) {
                bestCount = count
                bestBox = candidate
            }
        }
        return bestBox
    }

    private fun isSimilar(a: QuadBox, b: QuadBox): Boolean {
        fun Point.distanceTo(other: Point): Double =
            hypot(x - other.x, y - other.y)

        return a.topLeft.distanceTo(b.topLeft) < matchTolerance &&
                a.topRight.distanceTo(b.topRight) < matchTolerance &&
                a.bottomLeft.distanceTo(b.bottomLeft) < matchTolerance &&
                a.bottomRight.distanceTo(b.bottomRight) < matchTolerance
    }
}
