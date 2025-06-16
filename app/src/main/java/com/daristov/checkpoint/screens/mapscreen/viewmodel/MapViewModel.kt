package com.daristov.checkpoint.screens.mapscreen.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.screens.mapscreen.MapInitStep
import com.daristov.checkpoint.screens.mapscreen.domain.MapObject
import com.daristov.checkpoint.service.LocationRepository
import com.daristov.checkpoint.service.OverpassAPI
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import kotlin.collections.plus
import kotlin.math.floor

const val CUSTOMS_TILE_SIZE_DEGREES = 1.0
const val CUSTOMS_TILE_REQUEST_DELAY_MS = 100L
const val MIN_ZOOM_FOR_TILES_LOAD = 9.0

class MapViewModel : ViewModel() {

    private val overpassAPI: OverpassAPI = OverpassAPI()
    private val _location = MutableStateFlow<Location?>(null)
    private val _customs = MutableStateFlow<List<MapObject>>(emptyList())
    private val _currentStep = MutableStateFlow<MapInitStep>(MapInitStep.LOADING_MAP)
    private val _needToShowSurvey = MutableStateFlow<Boolean>(false)
    private val _isInitialZoomAndSurveyDone = MutableStateFlow<Boolean>(false)
    private val _visibleMarkerIds = MutableStateFlow<MutableSet<String>>(mutableSetOf())
    private val _existingMarkers = MutableStateFlow<MutableMap<String, Marker>>(mutableMapOf())

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
    val currentStep: StateFlow<MapInitStep> = _currentStep
    val isInitialZoomAndSurveyDone: StateFlow<Boolean> = _isInitialZoomAndSurveyDone
    val needToShowSurvey: StateFlow<Boolean> = _needToShowSurvey
    val visibleMarkerIds: StateFlow<MutableSet<String>> = _visibleMarkerIds
    val existingMarkers: StateFlow<MutableMap<String, Marker>> = _existingMarkers

    fun loadCustomsInVisibleArea(bounds: LatLngBounds, zoom: Double) {
        if (zoom < MIN_ZOOM_FOR_TILES_LOAD) {
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
                delay(CUSTOMS_TILE_REQUEST_DELAY_MS)
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

    fun findNearestCustoms(from: LatLng, count: Int = 4): List<MapObject> {
        return customs.value
            .sortedBy { it.location.distanceTo(from) }
            .take(count)
    }

    fun setCurrentStep(step: MapInitStep) {
        _currentStep.value = step
    }

    fun getCurrentStep(): MapInitStep {
        return currentStep.value
    }

    fun setInitialZoomAndSurveyDone(done: Boolean) {
        _isInitialZoomAndSurveyDone.value = done
    }

    fun setNeedToShowSurvey(show: Boolean) {
        _needToShowSurvey.value = show
    }

    fun getVisibleMarkerIds(): MutableSet<String> {
        return _visibleMarkerIds.value
    }

    fun getExistingMarkers(): MutableMap<String, Marker> {
        return _existingMarkers.value
    }

    fun removeVisibleMarkerId(id: String) {
        _visibleMarkerIds.value.remove(id)
    }

    fun removeExistingMarker(id: String) {
        _existingMarkers.value.remove(id)
    }

    fun addVisibleMarkerId(id: String) {
        _visibleMarkerIds.value.add(id)
    }

    fun addExistingMarker(id: String, marker: Marker) {
        _existingMarkers.value[id] = marker
    }

    fun containsVisibleMarkerId(id: String): Boolean {
        return _visibleMarkerIds.value.contains(id)
    }

    fun latToTileY(lat: Double): Int = floor(lat / CUSTOMS_TILE_SIZE_DEGREES).toInt()
    fun lonToTileX(lon: Double): Int = floor(lon / CUSTOMS_TILE_SIZE_DEGREES).toInt()

    fun TileKey.centerGeoPoint(): LatLng {
        val lat = (y + 0.5) * CUSTOMS_TILE_SIZE_DEGREES
        val lon = (x + 0.5) * CUSTOMS_TILE_SIZE_DEGREES
        return LatLng(lat, lon)
    }

    fun sendSurveyAnswer(answer: Int) {
        //TODO: send answer to server
    }

    data class TileKey(val x: Int, val y: Int) {
        override fun equals(other: Any?): Boolean {
            return other is TileKey && other.x == x && other.y == y
        }

        override fun hashCode(): Int {
            return 31 * x + y
        }
    }
}