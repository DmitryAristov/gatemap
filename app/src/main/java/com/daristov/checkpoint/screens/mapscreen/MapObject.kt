package com.daristov.checkpoint.screens.mapscreen

import org.osmdroid.util.GeoPoint

data class MapObject(
    val id: String,
    val name: String,
    val type: ObjectType,
    val latitude: Double,
    val longitude: Double
) {
    val location: GeoPoint get() = GeoPoint(latitude, longitude)
}

enum class ObjectType {
    CHECKPOINT
}
