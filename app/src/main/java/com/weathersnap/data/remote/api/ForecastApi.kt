package com.weathersnap.data.remote.api

import com.weathersnap.data.remote.dto.ForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,wind_speed_10m,pressure_msl,weather_code",
        @Query("timezone") timezone: String = "auto",
    ): ForecastResponse
}
