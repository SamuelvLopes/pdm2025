package com.weatherapp.model

import ForecastMonitor
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.weatherapp.api.WeatherService
import com.weatherapp.api.toForecast
import com.weatherapp.api.toWeather
import com.weatherapp.db.fb.FBCity
import com.weatherapp.db.fb.FBDatabase
import com.weatherapp.db.fb.FBUser
import com.weatherapp.db.fb.toFBCity
import com.weatherapp.ui.nav.Route


class MainViewModel(
    private val db: FBDatabase,
    private val service: WeatherService,
    private val monitor: ForecastMonitor
) : ViewModel(), FBDatabase.Listener {

    private var _page = mutableStateOf<Route>(Route.Home)
    var page: Route
        get() = _page.value
        set(tmp) {
            _page.value = tmp
        }

    private var _city = mutableStateOf<City?>(null)
    var city: City?
        get() = _city.value
        set(tmp) {
            _city.value = tmp?.copy()
        }
    private val _cities = mutableStateMapOf<String, City>()
    val cities: List<City>
        get() = _cities.values.toList()
    private val _user = mutableStateOf<User?>(null)
    val user: User?
        get() = _user.value

    init {
        db.setListener(this)
    }

    fun remove(city: City) {
        db.remove(city.toFBCity())
    }

    fun add(name: String, location: LatLng? = null) {
        db.add(City(name = name, location = location).toFBCity())
    }

    override fun onUserLoaded(user: FBUser) {
        _user.value = user.toUser()
    }

    override fun onUserSignOut() {
        monitor.cancelAll()
    }

    override fun onCityAdded(city: FBCity) {
        _cities[city.name!!] = city.toCity()
        monitor.updateCity(city.toCity())
    }


    override fun onCityUpdated(city: FBCity) {
        val oldCity = _cities[city.name]
        _cities.remove(city.name)
        _cities[city.name!!] = city.toCity().copy(
            weather = oldCity?.weather,
            forecast = oldCity?.forecast
        )
        if (_city.value?.name == city.name) {
            _city.value = _cities[city.name]
        }
        monitor.updateCity(city.toCity())
    }


    override fun onCityRemoved(city: FBCity) {
        _cities.remove(city.name)
        if (_city.value?.name == city.name) {
            _city.value = null
        }
        monitor.cancelCity(city.toCity())
    }

    private fun generateMockCities() = List(20) { i ->
        City(name = "Cidade $i")
    }

    fun add(name: String) {
        service.getLocation(name) { lat, lng ->
            if (lat != null && lng != null) {
                db.add(City(name = name, location = LatLng(lat, lng), isMonitored = true).toFBCity())
            }
        }
    }

    fun add(location: LatLng) {
        service.getName(location.latitude, location.longitude) { name ->
            if (name != null) {
                db.add(City(name = name, location = location, isMonitored = true).toFBCity())
            }
        }
    }


    class MainViewModelFactory(
        private val db: FBDatabase,
        private val service: WeatherService,
        private val monitor: ForecastMonitor
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(db, service, monitor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun loadWeather(name: String) {
        service.getWeather(name) { apiWeather ->
            val newCity = _cities[name]?.copy(weather = apiWeather?.toWeather())
            _cities.remove(name)
            _cities[name] = newCity as City
        }
    }

    fun loadForecast(name: String) {
        service.getForecast(name) { apiForecast ->
            val newCity = _cities[name]!!.copy(forecast = apiForecast?.toForecast())
            _cities.remove(name)
            _cities[name] = newCity
            city = if (city?.name == name) newCity else city
        }
    }

    fun loadBitmap(name: String)
    {
        val city = _cities[name]
        service.getBitmap(city?.weather!!.imgUrl) { bitmap ->
            val newCity = city.copy(
                weather = city.weather?.copy(
                    bitmap = bitmap
                )
            )
            _cities.remove(name)
            _cities[name] = newCity
        }
    }

   fun update(city: City){
       _city.value = city

       _cities[city.name] = city

       db.update(city.toFBCity())
   }
}
