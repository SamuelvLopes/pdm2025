package com.weatherapp.db.fb

import com.google.android.gms.maps.model.LatLng
import com.weatherapp.model.City

class FBCity {
    var name: String? = null
    var lat: Double? = null
    var lng: Double? = null
    var monitored: Boolean = false

    fun toCity(): City {
        val latlng = if (lat != null && lng != null) LatLng(lat!!, lng!!) else null
        return City(name!!, weather = null, location = latlng, isMonitored = monitored)
    }
}

fun City.toFBCity(): FBCity {
    val fbCity = FBCity()
    fbCity.name = this.name
    fbCity.lat = this.location?.latitude ?: 0.0
    fbCity.lng = this.location?.longitude ?: 0.0
    fbCity.monitored = this.isMonitored
    return fbCity
}
