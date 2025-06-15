package com.daristov.checkpoint.service

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationService : LifecycleService() {

    private lateinit var fusedClient: FusedLocationProviderClient

    override fun onCreate() {
        Log.d("LocationService", "Starting location updates")
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    override fun onDestroy() {
        Log.d("LocationService", "Destroying location updates")
        super.onDestroy()
    }

    private fun startLocationUpdates() {
        val intervalMills = 1 * 1000L
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMills).build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                LocationRepository.updateLocation(location)
                Log.d("LocationService", "Lat: ${location.latitude}, Lng: ${location.longitude}")
            }
        }
    }
}