package com.daristov.checkpoint.service

import android.util.Log
import com.daristov.checkpoint.screens.mapscreen.domain.MapObject
import com.daristov.checkpoint.screens.mapscreen.domain.ObjectType
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MapViewModel.TileKey
import com.daristov.checkpoint.screens.mapscreen.viewmodel.CUSTOMS_TILE_SIZE_DEGREES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.geojson.BoundingBox
import java.net.URL
import java.net.URLEncoder

class OverpassAPI {

    internal suspend fun loadTile(tile: TileKey): List<MapObject> {
        val tileBounds = tileToBoundingBox(tile)
        val result = withContext(Dispatchers.IO) {
            val query = """
                    [out:json][timeout:25];
                    node["barrier"="border_control"](${tileBounds.south()},${tileBounds.west()},${tileBounds.north()},${tileBounds.east()});
                    out body;
                """.trimIndent()

            val url = "https://overpass-api.de/api/interpreter?data=" +
                    URLEncoder.encode(query, "UTF-8")

            val response = URL(url).readText()
            return@withContext parseOverpassResponse(response)
        }

        Log.d("MapViewModel", "Loaded $result customs for tile $tile")
        return result
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
                    type = ObjectType.CUSTOMS,
                    latitude = lat,
                    longitude = lon
                )
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error parsing Overpass response", e)
        }

        return list
    }

    private fun tileToBoundingBox(tile: TileKey): BoundingBox {
        val south = tile.y * CUSTOMS_TILE_SIZE_DEGREES
        val north = (tile.y + 1) * CUSTOMS_TILE_SIZE_DEGREES
        val west = tile.x * CUSTOMS_TILE_SIZE_DEGREES
        val east = (tile.x + 1) * CUSTOMS_TILE_SIZE_DEGREES
        return BoundingBox.fromLngLats(west, south, east, north)
    }
}