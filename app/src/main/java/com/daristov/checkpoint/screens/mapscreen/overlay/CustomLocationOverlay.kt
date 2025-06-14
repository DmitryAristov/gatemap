package com.daristov.checkpoint.screens.mapscreen.overlay

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import androidx.core.graphics.withRotation

class CustomLocationOverlay : Overlay() {

    var location: GeoPoint? = null
    var bearing: Float = 0f
    var icon: Drawable? = null

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas == null || mapView == null || location == null || icon == null) return

        val projection = mapView.projection
        val point = projection.toPixels(location, null)
        val cx = point.x.toFloat()
        val cy = point.y.toFloat()

        canvas.withRotation(bearing, cx, cy) {
            icon?.setBounds(
                (cx - icon!!.intrinsicWidth / 2).toInt(),
                (cy - icon!!.intrinsicHeight / 2).toInt(),
                (cx + icon!!.intrinsicWidth / 2).toInt(),
                (cy + icon!!.intrinsicHeight / 2).toInt()
            )
            icon?.draw(this)
        }
    }

    fun update(location: Location) {
        this.location = GeoPoint(location.latitude, location.longitude)
        this.bearing = location.bearing
    }

    fun update(location: GeoPoint, bearing: Float) {
        this.location = location
        this.bearing = bearing
    }
}



