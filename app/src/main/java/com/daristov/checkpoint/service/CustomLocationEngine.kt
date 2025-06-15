package com.daristov.checkpoint.service

import android.app.PendingIntent
import android.os.Looper
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

class CustomLocationEngine: LocationEngine {
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult?>) {
//        LocationProvider.locationFlow.collect { location ->
//            callback.onSuccess(LocationEngineResult.create(location))
//        }
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult?>,
        looper: Looper?
    ) {
        TODO("Not yet implemented")
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        pendingIntent: PendingIntent?
    ) {
        TODO("Not yet implemented")
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult?>) {
        TODO("Not yet implemented")
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        TODO("Not yet implemented")
    }

}