package com.weathersnap.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
)
