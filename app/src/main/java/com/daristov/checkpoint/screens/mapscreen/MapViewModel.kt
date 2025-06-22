package com.daristov.checkpoint.screens.mapscreen

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daristov.checkpoint.service.LocationRepository
import com.daristov.checkpoint.service.GatemapAPI
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

const val CUSTOMS_TILE_SIZE_DEGREES = 3.0
const val CUSTOMS_TILE_REQUEST_DELAY_MS = 100L
const val MIN_ZOOM_FOR_TILES_LOAD = 9.0
const val SURVEY_DISMISSAL_TIMEOUT = 5_000L
const val SURVEY_REENABLE_TIMEOUT = 20_000L

class MapViewModel : ViewModel() {

    private val gatemapAPI: GatemapAPI = GatemapAPI()
    private val _location = MutableStateFlow<Location?>(null)
    private val _customs = MutableStateFlow<List<Checkpoint>>(emptyList())
    private val _currentStep = MutableStateFlow<MapInitStep>(MapInitStep.LOADING_MAP)
    private val _insideCustomArea = MutableStateFlow<Checkpoint?>(null)
    private val _isInitialZoomDone = MutableStateFlow<Boolean>(false)
    private val _visibleMarkerIds = MutableStateFlow<MutableSet<String>>(mutableSetOf())
    private val _selectedCustomId = MutableStateFlow<String?>(null)
    private val _isSurveyVisible = MutableStateFlow(true)

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

    val customs: StateFlow<List<Checkpoint>> = _customs
    val currentStep: StateFlow<MapInitStep> = _currentStep
    val isInitialZoomDone: StateFlow<Boolean> = _isInitialZoomDone
    val insideCustomArea: StateFlow<Checkpoint?> = _insideCustomArea
    val visibleMarkerIds: StateFlow<MutableSet<String>> = _visibleMarkerIds
    val selectedCustomId: StateFlow<String?> = _selectedCustomId
    val isSurveyVisible: StateFlow<Boolean> = _isSurveyVisible

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
                    val result = gatemapAPI.loadTile(tile)
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

    fun findNearestCustoms(from: LatLng, count: Int = 4): List<Checkpoint> {
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

    fun setInitialZoomDone(done: Boolean) {
        _isInitialZoomDone.value = done
    }

    fun setInsideCustomArea(custom: Checkpoint) {
        _insideCustomArea.value = custom
    }

    fun getVisibleMarkerIds(): MutableSet<String> {
        return _visibleMarkerIds.value
    }

    fun onCustomSelected(customId: String) {
        _selectedCustomId.value = customId
    }

    fun clearSelectedCustom() {
        _selectedCustomId.value = null
    }

    fun latToTileY(lat: Double): Int = floor(lat / CUSTOMS_TILE_SIZE_DEGREES).toInt()
    fun lonToTileX(lon: Double): Int = floor(lon / CUSTOMS_TILE_SIZE_DEGREES).toInt()

    fun TileKey.centerGeoPoint(): LatLng {
        val lat = (y + 0.5) * CUSTOMS_TILE_SIZE_DEGREES
        val lon = (x + 0.5) * CUSTOMS_TILE_SIZE_DEGREES
        return LatLng(lat, lon)
    }

    fun setSurveyVisible(visible: Boolean) {
        _isSurveyVisible.value = visible
    }

    fun dismissSurveyTemporarily() {
        _isSurveyVisible.value = false
        viewModelScope.launch {
            delay(SURVEY_REENABLE_TIMEOUT)
            _isSurveyVisible.value = true
        }
    }

    //TODO: send to server
    fun sendQueueSize(value: Int) {
        _isSurveyVisible.value = false
    }

    //TODO: send to server
    fun sendWaitTimeMinutes(value: Int) {
        _isSurveyVisible.value = false
    }

    fun openChatScreen(string: String) {
        return
        //TODO("Not yet implemented")
    }

    fun getCustomMapObject(customId: String): Checkpoint? {
        return customs.value.find { it.id == customId }
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

data class Checkpoint(
    val id: String,
    val name: String,
    val queueSize: Int = 0,
    val waitTimeHours: Int = 0,
    val latitude: Double,
    val longitude: Double
) {
    val location: LatLng get() = LatLng(latitude, longitude)
}
