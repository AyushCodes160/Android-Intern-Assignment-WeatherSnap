package com.weathersnap.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentDto? = null,
    @SerializedName("current_units") val currentUnits: CurrentUnitsDto? = null,
)

data class CurrentDto(
    val time: String,
    val temperature_2m: Double?,
    val relative_humidity_2m: Int?,
    val wind_speed_10m: Double?,
    val pressure_msl: Double?,
    val weather_code: Int?,
)

data class CurrentUnitsDto(
    val temperature_2m: String? = null,
    val wind_speed_10m: String? = null,
    val pressure_msl: String? = null,
)
