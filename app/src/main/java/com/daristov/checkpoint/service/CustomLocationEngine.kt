package com.daristov.checkpoint.service

import android.app.PendingIntent
import android.os.Looper
import com.daristov.checkpoint.screens.settings.AppTrackingMode
import com.daristov.checkpoint.util.MapScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

class CustomLocationEngine(
    private val locationComponent: LocationComponent,
    private val trackingMode: AppTrackingMode
) : LocationEngine {

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        LocationRepository.getLatestLocation()?.let {
            callback.onSuccess(LocationEngineResult.create(it))
        } ?: callback.onFailure(Exception("No location yet"))
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        LocationRepository.locationFlow
            .onEach {
                it?.let { location ->
                    callback.onSuccess(LocationEngineResult.create(location))
                    MapScreenUtils.updateRenderMode(locationComponent, trackingMode, location)
                }
            }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {  }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {  }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {  }
}