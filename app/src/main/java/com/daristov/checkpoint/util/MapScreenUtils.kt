package com.daristov.checkpoint.util

import android.content.Context
import android.location.Location
import androidx.annotation.DrawableRes
import androidx.compose.runtime.MutableState
import androidx.core.content.ContextCompat
import com.daristov.checkpoint.R
import com.daristov.checkpoint.screens.mapscreen.domain.MapObject
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MapViewModel
import com.daristov.checkpoint.screens.settings.AppThemeMode
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import androidx.core.graphics.drawable.toBitmap
import org.maplibre.android.annotations.Icon
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import kotlin.collections.set
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object MapScreenUtils {

    fun MapView.setupInteractionListeners(
        context: Context,
        viewModel: MapViewModel,
        theme: AppThemeMode,
        visibleMarkerIds: MutableSet<String>,
        existingMarkers: MutableMap<String, Marker>
    ) {
        getMapAsync { map ->
            val minIntervalMs = 100L
            var lastUpdate = 0L

            map.addOnCameraIdleListener {
                val now = System.currentTimeMillis()
                if (now - lastUpdate < minIntervalMs) return@addOnCameraIdleListener
                lastUpdate = now

                val bounds = map.projection.visibleRegion.latLngBounds
                val zoom = map.cameraPosition.zoom
                viewModel.loadCustomsInVisibleArea(bounds, zoom)

                val customs = viewModel.customs.value
                showVisibleCustoms(customs, context, theme, visibleMarkerIds, existingMarkers)
            }
        }
    }

    fun MapView.showVisibleCustoms(
        allCustoms: List<MapObject>,
        context: Context,
        theme: AppThemeMode,
        visibleMarkerIds: MutableSet<String>,
        existingMarkers: MutableMap<String, Marker>
    ) {
        getMapAsync { map ->
            val bounds = map.projection.visibleRegion.latLngBounds

            // 1. Удаляем маркеры, которые вышли за границы
            val toRemove = visibleMarkerIds.filterNot { id ->
                val custom = allCustoms.find { it.id == id } ?: return@filterNot false
                bounds.contains(LatLng(custom.latitude, custom.longitude))
            }
            toRemove.forEach { id ->
                existingMarkers[id]?.let { map.removeMarker(it) }
                existingMarkers.remove(id)
                visibleMarkerIds.remove(id)
            }

            // 2. Добавляем те, которые попали в границы
            allCustoms.forEach { custom ->
                if (visibleMarkerIds.contains(custom.id)) return@forEach

                val latLng = LatLng(custom.latitude, custom.longitude)
                if (bounds.contains(latLng)) {
                    val icon = createCustomIcon(
                        context,
                        if (theme == AppThemeMode.DARK)
                            R.drawable.ic_custom_dark
                        else
                            R.drawable.ic_custom_light
                    )

                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(custom.name)
                            .icon(icon)
                    )

                    existingMarkers[custom.id] = marker
                    visibleMarkerIds.add(custom.id)
                }
            }
        }
    }

    fun MapView.handleInitialZoomAndSurvey(
        location: Location,
        nearestCustom: MapObject,
        distance: Double,
        needToShowSurvey: MutableState<Boolean>,
        viewModel: MapViewModel
    ) {
        getMapAsync { map ->
            val userLatLng = LatLng(location.latitude, location.longitude)
            val targetLatLng = LatLng(nearestCustom.latitude, nearestCustom.longitude)

            if (distance < 5000) {
                map.zoomToCustoms(userLatLng, listOf(targetLatLng))
                needToShowSurvey.value = true
                return@getMapAsync
            }

            if (location.speed > 10) { // >36 км/ч
                val nearest = viewModel.findNearestCustoms(userLatLng)
                map.zoomToCustoms(userLatLng, nearest.map { LatLng(it.latitude, it.longitude) })
                return@getMapAsync
            }

            map.cameraPosition = CameraPosition.Builder()
                .tilt(50.0)
                .zoom(17.0)
                .build()
            map.locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
        }
    }

    fun MapLibreMap.zoomToCustoms(
        userLatLng: LatLng,
        nearest: List<LatLng>
    ) {
        val allPoints = nearest + userLatLng
        val bounds = LatLngBounds.Builder().apply {
            allPoints.forEach { include(it) }
        }.build().increaseByMargin(0.2)

        animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
    }

    fun LatLng.createBoundingBoxAround(distanceKm: Double): LatLngBounds {
        val latDegreeDelta = distanceKm / 111.0
        val lonDegreeDelta = distanceKm / (111.320 * cos(Math.toRadians(latitude)))
        val north = latitude + latDegreeDelta
        val south = latitude - latDegreeDelta
        val east = longitude + lonDegreeDelta
        val west = longitude - lonDegreeDelta
        return LatLngBounds.Builder()
            .include(LatLng(north, east))
            .include(LatLng(south, west))
            .build()
    }

    fun createCustomIcon(context: Context, @DrawableRes resId: Int): Icon {
        val drawable = ContextCompat.getDrawable(context, resId) ?: error("Drawable not found")
        val bitmap = drawable.toBitmap()
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    fun LatLngBounds.increaseByMargin(margin: Double): LatLngBounds {
        val latSpan = latitudeSpan * margin
        val lonSpan = longitudeSpan * margin
        return LatLngBounds.from(
            latitudeNorth + latSpan,
            longitudeEast + lonSpan,
            latitudeSouth - latSpan,
            longitudeWest - lonSpan
        )
    }
}