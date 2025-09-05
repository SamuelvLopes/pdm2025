package com.weatherapp.api

import com.weatherapp.APIForecastDay

data class APIForecast(var forecastday: List<APIForecastDay>? = null)