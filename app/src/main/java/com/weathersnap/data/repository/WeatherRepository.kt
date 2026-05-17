package com.weathersnap.data.repository

import com.weathersnap.data.cache.CitySuggestionCache
import com.weathersnap.data.remote.api.ForecastApi
import com.weathersnap.data.remote.api.GeocodingApi
import com.weathersnap.domain.model.City
import com.weathersnap.domain.model.WeatherCode
import com.weathersnap.domain.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val geocodingApi: GeocodingApi,
    private val forecastApi: ForecastApi,
    private val cache: CitySuggestionCache,
) {

    suspend fun searchCities(query: String): List<City> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length <= 2) return@withContext emptyList()

        cache.get(trimmed)?.let { return@withContext it }

        val response = geocodingApi.search(name = trimmed)
        val cities = response.results.orEmpty().map { r ->
            City(
                id = r.id,
                name = r.name,
                country = r.country,
                admin1 = r.admin1,
                latitude = r.latitude,
                longitude = r.longitude,
            )
        }
        cache.put(trimmed, cities)
        cities
    }

    suspend fun fetchCurrentWeather(city: City): WeatherSnapshot = withContext(Dispatchers.IO) {
        val response = forecastApi.current(latitude = city.latitude, longitude = city.longitude)
        val current = response.current
            ?: error("Weather data unavailable for ${city.name}")
        WeatherSnapshot(
            city = city,
            temperatureC = current.temperature_2m ?: 0.0,
            condition = WeatherCode.describe(current.weather_code),
            weatherCode = current.weather_code ?: -1,
            humidity = current.relative_humidity_2m ?: 0,
            windSpeedKmh = current.wind_speed_10m ?: 0.0,
            pressureHpa = current.pressure_msl ?: 0.0,
            fetchedAtEpochMillis = System.currentTimeMillis(),
        )
    }
}
