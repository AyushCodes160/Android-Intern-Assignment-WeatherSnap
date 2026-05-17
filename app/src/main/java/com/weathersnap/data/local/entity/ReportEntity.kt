package com.weathersnap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val cityName: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val condition: String,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeedKmh: Double,
    val pressureHpa: Double,
    val weatherFetchedAt: Long,
    val notes: String,
    val imagePath: String,
    val originalImageBytes: Long,
    val compressedImageBytes: Long,
    val savedAt: Long,
)
