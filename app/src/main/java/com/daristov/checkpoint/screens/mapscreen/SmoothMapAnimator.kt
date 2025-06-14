package com.daristov.checkpoint.screens.mapscreen

import android.location.Location
import android.view.Choreographer
import com.daristov.checkpoint.screens.mapscreen.overlay.CustomLocationOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.math.sqrt

class SmoothCameraAnimator(
    private val mapView: MapView,
    private val scope: CoroutineScope,
    private val locationFlow: StateFlow<Location?>,
    private val isFollowFlow: StateFlow<Boolean>
) {
    private var targetPoint: GeoPoint? = null
    private var targetZoom: Double = mapView.zoomLevelDouble
    private var targetBearing: Float = mapView.mapOrientation

    private var currentPoint: GeoPoint? = null
    private var currentZoom: Double = mapView.zoomLevelDouble
    private var currentBearing: Float = mapView.mapOrientation

    private var startTime: Long = 0L
    private var durationMs = 1000L

    private var isRunning = false

    fun start() {
        scope.launch {
            isFollowFlow.collect { follow ->
                if (follow) {
                    locationFlow.collect { location ->
                        if (location != null) {
                            updateTarget(location)
                        }
                    }
                } else {
                    stop()
                }
            }
        }
    }

    private fun updateTarget(location: Location) {
        val now = System.currentTimeMillis()

        startTime = now
        currentPoint = mapView.mapCenter as GeoPoint
        currentZoom = mapView.zoomLevelDouble
        currentBearing = mapView.mapOrientation

        targetPoint = GeoPoint(location.latitude, location.longitude)
        targetZoom = calculateZoomBySpeed(location.speed)
        targetBearing = (360 - location.bearing) % 360f

        if (!isRunning) {
            isRunning = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val now = System.currentTimeMillis()
            val t = ((now - startTime).toFloat() / durationMs).coerceIn(0f, 1f)

            val cp = currentPoint
            val tp = targetPoint
            if (cp != null && tp != null) {
                val lat = lerp(cp.latitude, tp.latitude, t)
                val lon = lerp(cp.longitude, tp.longitude, t)
                val zoom = lerp(currentZoom, targetZoom, t)
                val bearing = lerpAngle(currentBearing, targetBearing, t)

                val geoPoint = GeoPoint(lat, lon)
                if (isFollowFlow.value) {
                    mapView.controller.setCenter(geoPoint)
                    mapView.controller.setZoom(zoom)
                    mapView.mapOrientation = bearing
                    mapView.overlays.filterIsInstance<CustomLocationOverlay>()
                        .firstOrNull()
                        ?.update(geoPoint, ((360 - bearing) % 360))
                    mapView.invalidate()
                }
            }

            if (t < 1f && isFollowFlow.value) {
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
    }

    private fun lerp(a: Double, b: Double, t: Float): Double = a + (b - a) * t

    private fun lerpAngle(start: Float, end: Float, t: Float): Float {
        val delta = ((((end - start) % 360) + 540) % 360) - 180
        return (start + delta * t + 360) % 360
    }

    private fun calculateZoomBySpeed(speed: Float): Double {
        val clampedSpeed = speed.coerceIn(0f, 50f)
        val maxZoom = 20.0
        val minZoom = 14.0
        val factor = sqrt(clampedSpeed / 50.0)
        return maxZoom - (maxZoom - minZoom) * factor
    }
}

