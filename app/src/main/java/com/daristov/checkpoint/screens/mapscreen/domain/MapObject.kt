package com.daristov.checkpoint.screens.mapscreen.domain

import org.maplibre.android.geometry.LatLng

data class MapObject(
    val id: String,
    val name: String,
    val type: ObjectType,
    val latitude: Double,
    val longitude: Double
) {
    val location: LatLng get() = LatLng(latitude, longitude)
}

enum class ObjectType {
    CUSTOMS
}
