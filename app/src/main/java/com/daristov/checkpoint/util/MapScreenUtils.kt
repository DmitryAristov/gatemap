package com.daristov.checkpoint.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.SizeF
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
import com.daristov.checkpoint.screens.mapscreen.CUSTOMS_AREA_RADIUS
import com.daristov.checkpoint.screens.mapscreen.DEFAULT_MAP_ANIMATION_DURATION
import com.daristov.checkpoint.screens.mapscreen.DEFAULT_TILT
import com.daristov.checkpoint.screens.mapscreen.DEFAULT_ZOOM
import com.daristov.checkpoint.service.CustomLocationEngine
import org.maplibre.android.annotations.Icon
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.CancelableCallback
import kotlin.collections.set
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.sin

object MapScreenUtils {

    fun MapLibreMap.setupInteractionListeners(
        context: Context,
        viewModel: MapViewModel,
        theme: AppThemeMode,
        visibleMarkerIds: MutableSet<String>,
        existingMarkers: MutableMap<String, Marker>
    ) {
        val minIntervalMs = 100L
        var lastUpdate = 0L

        addOnCameraIdleListener {
            val now = System.currentTimeMillis()
            if (now - lastUpdate < minIntervalMs) return@addOnCameraIdleListener
            lastUpdate = now

            val bounds = projection.visibleRegion.latLngBounds
            val zoom = cameraPosition.zoom
            viewModel.loadCustomsInVisibleArea(bounds, zoom)

            val customs = viewModel.customs.value
            showVisibleCustoms(customs, context, theme, visibleMarkerIds, existingMarkers)
        }
    }

    fun MapLibreMap.showVisibleCustoms(
        allCustoms: List<MapObject>,
        context: Context,
        theme: AppThemeMode,
        visibleMarkerIds: MutableSet<String>,
        existingMarkers: MutableMap<String, Marker>
    ) {
        val bounds = projection.visibleRegion.latLngBounds

        // 1. Удаляем маркеры, которые вышли за границы
        val toRemove = visibleMarkerIds.filterNot { id ->
            val custom = allCustoms.find { it.id == id } ?: return@filterNot false
            bounds.contains(LatLng(custom.latitude, custom.longitude))
        }
        toRemove.forEach { id ->
            existingMarkers[id]?.let { removeMarker(it) }
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

                val marker = addMarker(
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

    @SuppressLint("MissingPermission")
    fun MapLibreMap.loadLocationComponent(context: Context) {
        locationComponent.apply {
            val customLocationEngine = CustomLocationEngine()
            style?.let {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, it)
                        .useSpecializedLocationLayer(true)
                        .locationEngine(customLocationEngine)
                        .build()
                )
                isLocationComponentEnabled = true
                renderMode = RenderMode.COMPASS
                cameraMode = CameraMode.NONE
                val options = LocationComponentOptions.builder(context)
                    .pulseEnabled(true)
                    .accuracyAnimationEnabled(true)
                    .compassAnimationEnabled(true)
                    .trackingGesturesManagement(false)
                    .pulseMaxRadius(50f)
                    .build()
                applyStyle(options)
            }
        }
    }

    fun MapView.handleInitialZoomAndSurvey(
        location: Location,
        needToShowSurvey: MutableState<Boolean>,
        viewModel: MapViewModel
    ) {
        val userLatLng = LatLng(location)
        val nearestCustom = viewModel.findNearestCustoms(userLatLng, 1).first()
        val nearestCustomLatLng = LatLng(nearestCustom.latitude, nearestCustom.longitude)
        val distance = userLatLng.distanceTo(nearestCustomLatLng)

        if (distance < CUSTOMS_AREA_RADIUS) {
            needToShowSurvey.value = true
            val nearest = viewModel.findNearestCustoms(userLatLng, 1)
            zoomToCustoms(userLatLng, nearest.map { LatLng(it.latitude, it.longitude) })
        } else {
            val nearest = viewModel.findNearestCustoms(userLatLng)
            zoomToCustoms(userLatLng, nearest.map { LatLng(it.latitude, it.longitude) })
        }
    }

    fun MapView.zoomToCustoms(
        userLatLng: LatLng,
        nearest: List<LatLng>,
    ) {
        if (nearest.isEmpty()) return

        val allPoints = nearest + userLatLng

        val bounds = LatLngBounds.Builder().apply {
            allPoints.forEach { include(it) }
        }.build()

        val width = this.width
        val height = this.height

        val paddingPx = 100
        val zoom = CameraUpdateFactory.zoomForBounds(bounds, width - 2 * paddingPx, height - 2 * paddingPx)

        // Направление от пользователя к первому КПП
        val heading = computeHeading(userLatLng, nearest.first())
        val bearing = (heading + 360) % 360
        val target = nearest.first()
        val t = 0.7
        val offset = LatLng(
            latitude = target.latitude + (userLatLng.latitude - target.latitude) * t,
            longitude = target.longitude + (userLatLng.longitude - target.longitude) * t
        )

        getMapAsync { map ->
            val cameraPosition = CameraPosition.Builder()
                .target(offset)
                .zoom(zoom)
                .bearing(bearing)
                .tilt(DEFAULT_TILT)
                .build()

            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), DEFAULT_MAP_ANIMATION_DURATION)
        }
    }

    fun CameraUpdateFactory.zoomForBounds(
        bounds: LatLngBounds,
        mapWidthPx: Int,
        mapHeightPx: Int
    ): Double {
        val WORLD_DIM = SizeF(256f, 256f)
        val ZOOM_MAX = 22

        val ne = bounds.northEast
        val sw = bounds.southWest

        fun latRad(lat: Double): Double {
            val sin = sin(lat * PI / 180.0)
            return ln((1 + sin) / (1 - sin)) / 2.0
        }

        fun zoom(mapPx: Int, worldPx: Float, fraction: Double): Double {
            return floor(log2(mapPx / worldPx / fraction))
        }

        val latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / PI
        val lngDiff = (ne.longitude - sw.longitude + 360) % 360
        val lngFraction = lngDiff / 360.0

        val effectiveMapWidth = mapWidthPx - 2 * 100
        val effectiveMapHeight = mapHeightPx - 2 * 100

        val latZoom = zoom(effectiveMapHeight, WORLD_DIM.height, latFraction)
        val lngZoom = zoom(effectiveMapWidth, WORLD_DIM.width, lngFraction)

        return minOf(latZoom, lngZoom, ZOOM_MAX.toDouble()) * 0.9
    }

    fun computeHeading(from: LatLng, to: LatLng): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return Math.toDegrees(atan2(y, x))
    }

    fun MapView.setupTrackingButton(location: Location) {
        getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .tilt(DEFAULT_TILT)
                        .zoom(DEFAULT_ZOOM)
                        .target(LatLng(location))
                        .build()
                ),
                DEFAULT_MAP_ANIMATION_DURATION,
                object : CancelableCallback {
                    override fun onFinish() {
                        if (map.locationComponent.isLocationComponentActivated)
                            map.locationComponent.cameraMode =
                                CameraMode.TRACKING_COMPASS
                    }

                    override fun onCancel() {
                        return
                    }
                }
            )
        }
    }

    fun Context.getMapLibreMapStyleURL(
        theme: AppThemeMode,
        isSystemInDarkTheme: Boolean
    ): String {
        val apiKey = this.getString(R.string.maptiler_api_key)
        val darkStyleUrl = "https://api.maptiler.com/maps/dataviz-dark/style.json?key=$apiKey"
        val lightStyleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$apiKey"
        return when (theme) {
            AppThemeMode.DARK -> darkStyleUrl
            AppThemeMode.LIGHT -> lightStyleUrl
            AppThemeMode.SYSTEM -> {
                if (isSystemInDarkTheme) {
                    return darkStyleUrl
                } else {
                    return lightStyleUrl
                }
            }
        }
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
}