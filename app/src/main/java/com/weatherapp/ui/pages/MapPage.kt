package com.weatherapp.ui.pages

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.weatherapp.model.MainViewModel
import com.google.android.gms.maps.model.MapStyleOptions
import com.weatherapp.R


@Composable
fun MapPage(viewModel: MainViewModel) {
    val cityList = viewModel.cities
    val recife = LatLng(-8.05, -34.9)
    val caruaru = LatLng(-8.27, -35.98)
    val joaopessoa = LatLng(-7.12, -34.84)
    val camPosState = rememberCameraPositionState()
    val context = LocalContext.current
    val hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val mapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_night_style)



    GoogleMap(
        modifier = Modifier.fillMaxSize(), onMapClick = {
            viewModel.add("Cidade@${it.latitude}:${it.longitude}", location = it)
        }, cameraPositionState = camPosState,
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = mapStyle
        ),
        uiSettings = MapUiSettings(myLocationButtonEnabled = true)
    ) {

        viewModel.cities.forEach {
            if (it.location != null) {
                LaunchedEffect(it.name) {
                    if (it.weather == null) {
                        viewModel.loadWeather(it.name)
                    }
                }
                LaunchedEffect(it.weather) {
                    if (it.weather != null && it.weather!!.bitmap == null) {
                        viewModel.loadBitmap(it.name)
                    }
                }
                val image = it.weather?.bitmap ?:
                getDrawable(context, R.drawable.loading)!!
                    .toBitmap()
                val marker = BitmapDescriptorFactory
                    .fromBitmap(image.scale(120,120))
                Marker( state = MarkerState(position = it.location!!),
                    title = it.name,
                    icon = marker,
                    snippet = it.weather?.desc?:"Carregando..."
                )
            }
        }
    }
}