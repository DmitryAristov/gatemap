package com.daristov.checkpoint.screens.mapscreen.overlay

import android.location.Location
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class CustomLocationOverlay(provider: IMyLocationProvider?, mapView: MapView) :
    MyLocationNewOverlay(provider, mapView) {

    fun updateLocation(location: Location) {
        this.mDrawAccuracyEnabled = false
        this.isEnabled = true
        this.setLocation(location)
    }
}


