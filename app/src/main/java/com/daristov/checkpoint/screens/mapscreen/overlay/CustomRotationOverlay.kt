package com.daristov.checkpoint.screens.mapscreen.overlay

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.atan2
import kotlin.math.hypot

class CustomRotationOverlay(
    private val mapView: MapView,
    private val zoomSensitivity: Double = 0.002
) : Overlay() {

    private var active = false
    private var startAngle = 0.0
    private var startDistance = 0.0
    private var initialOrientation = 0f
    private var initialZoom = 0.0

    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        if (event.pointerCount != 2) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    startAngle = getAngle(event)
                    startDistance = getDistance(event)
                    initialOrientation = mapView.mapOrientation
                    initialZoom = mapView.zoomLevelDouble
                    active = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (active && event.pointerCount >= 2) {
                    val currentAngle = getAngle(event)
                    val deltaAngle = currentAngle - startAngle
                    val currentDistance = getDistance(event)
                    val deltaZoom = (currentDistance - startDistance) * zoomSensitivity
                    mapView.controller.setZoom(initialZoom + deltaZoom)

                    mapView.mapOrientation = (mapView.mapOrientation + deltaAngle).toFloat()
                    mapView.invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                active = false
            }
        }

        return false
    }

    private fun getAngle(event: MotionEvent): Double {
        val dx = (event.getX(1) - event.getX(0)).toDouble()
        val dy = (event.getY(1) - event.getY(0)).toDouble()
        return Math.toDegrees(atan2(dy, dx))
    }

    private fun getDistance(event: MotionEvent): Double {
        val dx = (event.getX(1) - event.getX(0)).toDouble()
        val dy = (event.getY(1) - event.getY(0)).toDouble()
        return hypot(dx, dy)
    }

    private var lastRotationTime = 0L

    private fun shouldUpdateRotation(): Boolean {
        val now = System.nanoTime()
        return if (now - lastRotationTime > 20000000) { // ограничим до ~60fps (1000/60 ≈ 16ms)
            lastRotationTime = now
            true
        } else {
            false
        }
    }
}

