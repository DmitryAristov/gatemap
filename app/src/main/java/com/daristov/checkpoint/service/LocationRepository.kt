package com.daristov.checkpoint.service

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocationRepository {
    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    fun updateLocation(location: Location) {
        _locationFlow.value = location
    }

    fun getLatestLocation(): Location? = _locationFlow.value
}
