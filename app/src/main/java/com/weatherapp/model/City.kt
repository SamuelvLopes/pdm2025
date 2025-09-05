package com.weatherapp.model

import Forecast
import com.google.android.gms.maps.model.LatLng

data class City(
    val name: String,
    var location: LatLng? = null,
    val weather: Weather? = null,
    val forecast: List<Forecast>? = null,
    val isMonitored: Boolean = false
)