package com.weathersnap.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherSnapshot(
    val city: City,
    val temperatureC: Double,
    val condition: String,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeedKmh: Double,
    val pressureHpa: Double,
    val fetchedAtEpochMillis: Long,
)
