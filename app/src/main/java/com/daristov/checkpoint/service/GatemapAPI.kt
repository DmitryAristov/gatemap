package com.daristov.checkpoint.service

import android.util.Log
import com.daristov.checkpoint.screens.mapscreen.MapViewModel.TileKey
import com.daristov.checkpoint.screens.mapscreen.CUSTOMS_TILE_SIZE_DEGREES
import com.daristov.checkpoint.screens.mapscreen.Checkpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.maplibre.geojson.BoundingBox

private const val API_URL = "https://gatemap.dev/checkpoints"

class GatemapAPI {

    private val client = OkHttpClient()

    internal suspend fun loadTile(tile: TileKey): List<Checkpoint> {
        val tileBounds = tileToBoundingBox(tile)

        return withContext(Dispatchers.IO) {
            val url = API_URL +
                    "?min_lat=${tileBounds.south()}" +
                    "&max_lat=${tileBounds.north()}" +
                    "&min_lon=${tileBounds.west()}" +
                    "&max_lon=${tileBounds.east()}"

            try {
                val request = Request.Builder().url(url).build()
                Log.d("MapViewModel", "Requesting $url")
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("MapViewModel", "Request failed: ${response.code}")
                        return@withContext emptyList()
                    }

                    val body = response.body?.string()
                    if (body.isNullOrBlank()) {
                        Log.e("MapViewModel", "Empty response body from $url")
                        return@withContext emptyList()
                    }

                    val array = JSONArray(body)
                    val checkpoints = List(array.length()) { i ->
                        val obj = array.getJSONObject(i)
                        Checkpoint(
                            id = obj.getString("id"),
                            name = obj.optString("name", "Customs"),
                            latitude = obj.getDouble("latitude"),
                            longitude = obj.getDouble("longitude"),
                            queueSize = obj.optInt("queueSize", 0),
                            waitTimeHours = obj.optInt("waitTimeHours", 0)
                        )
                    }

                    Log.d("MapViewModel", "Loaded ${checkpoints.size} checkpoints for tile $tile")
                    return@withContext checkpoints
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error loading tile $tile: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }

    private fun tileToBoundingBox(tile: TileKey): BoundingBox {
        val south = tile.y * CUSTOMS_TILE_SIZE_DEGREES
        val north = (tile.y + 1) * CUSTOMS_TILE_SIZE_DEGREES
        val west = tile.x * CUSTOMS_TILE_SIZE_DEGREES
        val east = (tile.x + 1) * CUSTOMS_TILE_SIZE_DEGREES
        return BoundingBox.fromLngLats(west, south, east, north)
    }
}