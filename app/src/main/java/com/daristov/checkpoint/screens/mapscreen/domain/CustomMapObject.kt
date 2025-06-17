package com.daristov.checkpoint.screens.mapscreen.domain

import org.maplibre.android.geometry.LatLng

data class CustomMapObject(
    val id: String,
    val name: String,
    val queueSize: Int = 0,
    val waitTimeMinutes: Int = 0,
    val latitude: Double,
    val longitude: Double
) {
    val location: LatLng get() = LatLng(latitude, longitude)
}
