package com.daristov.checkpoint.screens.mapscreen.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.screens.mapscreen.domain.MapObject
import com.daristov.checkpoint.service.LocationRepository
import com.daristov.checkpoint.service.OverpassAPI
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import kotlin.collections.plus
import kotlin.math.floor

const val TILE_SIZE_DEGREES = 1.0
private const val MIN_ZOOM_FOR_TILES = 9.0
private const val TILE_REQUEST_DELAY_MS = 100L

class MapViewModel : ViewModel() {

    private val overpassAPI: OverpassAPI = OverpassAPI()
    private val _location = MutableStateFlow<Location?>(null)
    private val _customs = MutableStateFlow<List<MapObject>>(emptyList())
    private val _nearestCustom = MutableStateFlow<MapObject?>(null)
    private val _distanceToNearestCustom = MutableStateFlow<Double>(-1.0)
    private val loadedTiles = mutableSetOf<TileKey>()
    private val loadingTiles = mutableSetOf<TileKey>()
    private val pendingTilesQueue = Channel<TileKey>(Channel.Factory.UNLIMITED)

    init {
        viewModelScope.launch {
            LocationRepository.locationFlow.collect { loc ->
                _location.value = loc
            }
        }
        startTileLoader()
    }

    val customs: StateFlow<List<MapObject>> = _customs
    val nearestCustom: StateFlow<MapObject?> = _nearestCustom
    val distanceToNearestCustom: StateFlow<Double> = _distanceToNearestCustom

    fun loadCustomsInVisibleArea(bounds: LatLngBounds, zoom: Double) {
        if (zoom < MIN_ZOOM_FOR_TILES) {
            return
        }
        val centerPoint = bounds.center

        val newTiles = getTilesInBounds(bounds)
            .filter { it !in loadedTiles && it !in loadingTiles }
            .sortedBy { it.centerGeoPoint().distanceTo(centerPoint) }

        newTiles.forEach {
            loadingTiles.add(it)
            pendingTilesQueue.trySend(it)
        }
    }

    private fun startTileLoader() {
        viewModelScope.launch {
            for (tile in pendingTilesQueue) {
                delay(TILE_REQUEST_DELAY_MS)
                try {
                    val result = overpassAPI.loadTile(tile)
                    _customs.update { it + result }
                    loadedTiles.add(tile)
                } catch (e: Exception) {
                    Log.e("MapViewModel", "Error loading tile $tile", e)
                } finally {
                    loadingTiles.remove(tile)
                }
            }
        }
    }

    private fun getTilesInBounds(bounds: LatLngBounds): List<TileKey> {
        val minX = lonToTileX(bounds.longitudeWest)
        val maxX = lonToTileX(bounds.longitudeEast)
        val minY = latToTileY(bounds.latitudeSouth)
        val maxY = latToTileY(bounds.latitudeNorth)

        return buildList {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    add(TileKey(x, y))
                }
            }
        }
    }

    fun updateNearestCustom(nearest: MapObject) {
        _nearestCustom.value = nearest
    }

    fun updateDistanceToNearestCustom(distance: Double) {
        _distanceToNearestCustom.value = distance
    }

    data class TileKey(val x: Int, val y: Int) {
        override fun equals(other: Any?): Boolean {
            return other is TileKey && other.x == x && other.y == y
        }

        override fun hashCode(): Int {
            return 31 * x + y
        }
    }

    fun findNearestCustoms(
        from: LatLng,
        count: Int = 4
    ): List<MapObject> {
        return customs.value
            .sortedBy { it.location.distanceTo(from) }
            .take(count)
    }

    fun latToTileY(lat: Double): Int = floor(lat / TILE_SIZE_DEGREES).toInt()
    fun lonToTileX(lon: Double): Int = floor(lon / TILE_SIZE_DEGREES).toInt()

    fun TileKey.centerGeoPoint(): LatLng {
        val lat = (y + 0.5) * TILE_SIZE_DEGREES
        val lon = (x + 0.5) * TILE_SIZE_DEGREES
        return LatLng(lat, lon)
    }

    fun sendSurveyAnswer(answer: Int) {
        //TODO: send answer to server
    }

    fun findAndSetNearestCustom(userPoint: LatLng) {
        val nearestCustom = findNearestCustoms(userPoint, 1).first()
        val cpPoint = LatLng(nearestCustom.latitude, nearestCustom.longitude)
        val distance = userPoint.distanceTo(cpPoint)
        updateDistanceToNearestCustom(distance)
        updateNearestCustom(nearestCustom)
    }
}