package com.daristov.checkpoint.screens.mapscreen.overlay

import android.location.Location
import com.daristov.checkpoint.service.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

class CustomLocationProvider : IMyLocationProvider {

    private var lastLocation: Location? = null
    private var consumer: IMyLocationConsumer? = null
    private var scope: CoroutineScope? = null

    override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
        consumer = myLocationConsumer

        scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        scope?.launch {
            LocationProvider.locationFlow.collect { location ->
                lastLocation = location
//                consumer?.onLocationChanged(location, this@CustomLocationProvider)
            }
        }

        return true
    }

    override fun stopLocationProvider() {
        scope?.cancel()
        scope = null
    }

    override fun getLastKnownLocation(): Location? {
        return lastLocation
    }

    override fun destroy() {
        stopLocationProvider()
    }
}
