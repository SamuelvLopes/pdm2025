package com.weatherapp

import com.weatherapp.api.APIWeather

data class APIForecastDay(var date: String? = null, var day: APIWeather? = null)
