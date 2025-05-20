package com.daristov.checkpoint.domain.model

import org.opencv.core.Point

data class QuadBox(val topLeft: Point, val topRight: Point, val bottomRight: Point, val bottomLeft: Point)
