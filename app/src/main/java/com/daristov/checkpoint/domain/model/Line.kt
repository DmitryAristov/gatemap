package com.daristov.checkpoint.domain.model

import kotlin.math.max
import kotlin.math.min

data class Line(val x1: Int, val y1: Int, val x2: Int, val y2: Int) {
    fun centerX() = (x1 + x2) / 2
    fun centerY() = (y1 + y2) / 2
    fun minY(): Int = min(y1, y2)
    fun maxY(): Int = max(y1, y2)
}