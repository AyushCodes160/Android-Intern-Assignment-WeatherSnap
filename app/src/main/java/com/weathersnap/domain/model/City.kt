package com.weathersnap.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class City(
    val id: Long,
    val name: String,
    val country: String?,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double,
) {
    val displayLine: String
        get() = listOfNotNull(name, admin1, country).joinToString(", ")

    val titleLine: String
        get() = listOfNotNull(name, country).joinToString(", ")
}
