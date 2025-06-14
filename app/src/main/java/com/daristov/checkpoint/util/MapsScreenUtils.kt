package com.daristov.checkpoint.util

import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.core.content.ContextCompat
import com.daristov.checkpoint.R
import com.daristov.checkpoint.screens.mapscreen.domain.MapObject
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MapViewModel
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object MapsScreenUtils {

    fun MapView.handleInitialZoomAndSurvey(
        location: Location,
        nearestCustom: MapObject,
        distance: Double,
        needToShowSurvey: MutableState<Boolean>,
        viewModel: MapViewModel
    ) {
        val userPoint = GeoPoint(location)
        if (distance < 5000) {
            val heading = computeHeading(userPoint, nearestCustom.location)
            controller.animateTo(userPoint, 18.0, 1000L, ((heading + 90 + 360) % 360).toFloat())
            needToShowSurvey.value = true
            return
        }

        if (location.speed > 10) { // ~36 км/ч
            var nearest: List<MapObject> = viewModel.findNearestCustoms(userPoint)
            this.zoomToCustoms(userPoint, nearest.map { GeoPoint(it.latitude, it.longitude) })
            return
        }

        this.controller.animateTo(userPoint, 18.0, 1000L)
        viewModel.enableFollow()
    }

    fun MapView.setupInteractionListeners(
        viewModel: MapViewModel,
        context: Context,
        customs: List<MapObject>
    ) {
        val minIntervalMs = 500L
        var lastCheckTime = 0L

        this.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val now = System.currentTimeMillis()
                if (now - lastCheckTime < minIntervalMs) return false
                lastCheckTime = now

                viewModel.loadCustomsInVisibleArea(boundingBox, zoomLevelDouble)
                showCustoms(customs, context)
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val now = System.currentTimeMillis()
                if (now - lastCheckTime < minIntervalMs) return false
                lastCheckTime = now

                viewModel.loadCustomsInVisibleArea(boundingBox, zoomLevelDouble)
                showCustoms(customs, context)
                return true
            }
        })
    }

    fun MapView.showCustoms(customs: List<MapObject>, context: Context) {
        val visibleBox = this.boundingBox
        overlays.removeAll { it is Marker }

        for (custom in customs) {
            val point = GeoPoint(custom.latitude, custom.longitude)
            if (visibleBox.contains(point)) {
                val marker = Marker(this).apply {
                    position = point
                    title = custom.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//                  Icons.Default.AssuredWorkload
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_custom) // или стандартная иконка
                    isDraggable = false
                    setOnMarkerClickListener { marker, _ ->
                        //TODO
                        Toast.makeText(context, "КПП: ${marker.title}", Toast.LENGTH_SHORT).show()
                        true
                    }
                }
                overlays.add(marker)
            }
        }

        invalidate()
    }

    fun MapView.zoomToCustoms(
        userPoint: GeoPoint, nearest: List<GeoPoint>
    ) {
        val bounds = BoundingBox.fromGeoPointsSafe(nearest + userPoint).increaseByMargin(0.1) // 10% запас
//        val target = nearest.first()
//        mapOrientation = ((computeHeading(userPoint, target) + 90 + 360) % 360).toFloat()
        this.zoomToBoundingBox(bounds, true)
    }

    fun GeoPoint.createBoundingBoxAround(distanceKm: Double): BoundingBox {
        val latDegreeDelta = distanceKm / 111.0 // приближённо 111 км на градус широты
        // для долготы учитываем сжатие по широте
        val lonDegreeDelta = distanceKm / (111.320 * cos(Math.toRadians(latitude)))
        return BoundingBox(
            latitude + latDegreeDelta,
            longitude + lonDegreeDelta,
            latitude - latDegreeDelta,
            longitude - lonDegreeDelta
        )
    }

    fun BoundingBox.increaseByMargin(marginFraction: Double): BoundingBox {
        val latSpan = this.latitudeSpan
        val lonSpan = this.longitudeSpanWithDateLine

        val latMargin = latSpan * marginFraction
        val lonMargin = lonSpan * marginFraction

        return BoundingBox(
            this.latNorth + latMargin,
            this.lonEast + lonMargin,
            this.latSouth - latMargin,
            this.lonWest - lonMargin
        )
    }

    fun computeHeading(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return Math.toDegrees(atan2(y, x))
    }
}
