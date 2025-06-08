package com.daristov.checkpoint.screens.mapscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.net.URL
import java.net.URLEncoder
import kotlin.math.floor

private const val TILE_SIZE_DEGREES = 2.0
private const val MIN_ZOOM_FOR_TILES = 10.0
private const val TILE_REQUEST_DELAY_MS = 100L

class MapViewModel : ViewModel() {

    private val _checkpoints = MutableStateFlow<List<MapObject>>(emptyList())
    private val loadedTiles = mutableSetOf<TileKey>()
    private val loadingTiles = mutableSetOf<TileKey>()
    private val pendingTilesQueue = Channel<TileKey>(Channel.UNLIMITED)
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)

    val currentLocation: StateFlow<GeoPoint?> = _currentLocation
    val checkpoints: StateFlow<List<MapObject>> = _checkpoints

    init {
        startTileLoader()
    }

    fun loadCheckpointsInVisibleArea(bounds: BoundingBox, zoom: Double) {
        if (zoom < MIN_ZOOM_FOR_TILES) {
            Log.d("MapViewModel", "Zoom $zoom слишком мал, загрузка тайлов пропущена")
            return
        }

        val newTiles = getTilesInBounds(bounds)
            .filter { it !in loadedTiles && it !in loadingTiles }
            .toSet()

        val allTiles = getTilesInBounds(bounds)
        Log.d("MapViewModel", "Visible area bounds: $bounds")
        Log.d("MapViewModel", "Computed tiles: $allTiles")
        Log.d("MapViewModel", "New tiles to load: $newTiles")

        newTiles.forEach {
            loadingTiles.add(it)  // отмечаем как в процессе загрузки
            pendingTilesQueue.trySend(it)
        }
    }

    private fun startTileLoader() {
        viewModelScope.launch {
            for (tile in pendingTilesQueue) {
                delay(TILE_REQUEST_DELAY_MS)
                loadTile(tile)
            }
        }
    }

    private fun getTilesInBounds(bounds: BoundingBox): List<TileKey> {
        val minX = lonToTileX(bounds.lonWest)
        val maxX = lonToTileX(bounds.lonEast)
        val minY = latToTileY(bounds.latSouth)
        val maxY = latToTileY(bounds.latNorth)

        return buildList {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    add(TileKey(x, y))
                }
            }
        }
    }

    private fun tileToBoundingBox(tile: TileKey): BoundingBox {
        val south = tile.y * TILE_SIZE_DEGREES
        val north = (tile.y + 1) * TILE_SIZE_DEGREES
        val west = tile.x * TILE_SIZE_DEGREES
        val east = (tile.x + 1) * TILE_SIZE_DEGREES
        return BoundingBox(north, east, south, west)
    }

    private suspend fun loadTile(tile: TileKey) {
        try {
            val tileBounds = tileToBoundingBox(tile)
            val result = withContext(Dispatchers.IO) {
                val query = """
                    [out:json][timeout:25];
                    node["barrier"="border_control"](${tileBounds.latSouth},${tileBounds.lonWest},${tileBounds.latNorth},${tileBounds.lonEast});
                    out body;
                """.trimIndent()

                val url = "https://overpass-api.de/api/interpreter?data=" +
                        URLEncoder.encode(query, "UTF-8")

                val response = URL(url).readText()
                return@withContext parseOverpassResponse(response)
            }

            _checkpoints.update { it + result }
            Log.d("MapViewModel", "Loaded $result checkpoints for tile $tile")
            loadedTiles.add(tile)
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error loading tile $tile", e)
        } finally {
            loadingTiles.remove(tile)
        }
    }

    fun parseOverpassResponse(json: String): List<MapObject> {
        val list = mutableListOf<MapObject>()

        try {
            val root = JSONObject(json)
            val elements = root.getJSONArray("elements")

            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)

                // Пропускаем если нет координат
                val lat = el.optDouble("lat", Double.NaN)
                val lon = el.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue

                val tags = el.optJSONObject("tags")
                val name = tags?.optString("name", "КПП") ?: "КПП"

                list += MapObject(
                    id = el.optLong("id", 0L).toString(),
                    name = name,
                    type = ObjectType.CHECKPOINT,
                    latitude = lat,
                    longitude = lon
                )
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error parsing Overpass response", e)
        }

        return list
    }

    fun updateCurrentLocation(geo: GeoPoint?) {
        if (geo != null) {
            _currentLocation.value = geo
        }
    }

    data class TileKey(val x: Int, val y: Int) {
        override fun equals(other: Any?): Boolean {
            return other is TileKey && other.x == x && other.y == y
        }

        override fun hashCode(): Int {
            return 31 * x + y
        }
    }

    fun findNearestCheckpoints(
        from: GeoPoint,
        count: Int = 4
    ): List<MapObject> {
        return checkpoints.value
            .sortedBy { it.location.distanceToAsDouble(from) }
            .take(count)
    }

    fun latToTileY(lat: Double): Int = floor(lat / TILE_SIZE_DEGREES).toInt()
    fun lonToTileX(lon: Double): Int = floor(lon / TILE_SIZE_DEGREES).toInt()
}
