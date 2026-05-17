package com.weathersnap.data.repository

import androidx.room.withTransaction
import com.weathersnap.data.local.WeatherSnapDatabase
import com.weathersnap.data.local.dao.DraftDao
import com.weathersnap.data.local.dao.ReportDao
import com.weathersnap.data.local.entity.ReportEntity
import com.weathersnap.domain.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val db: WeatherSnapDatabase,
    private val reportDao: ReportDao,
    private val draftDao: DraftDao,
) {

    fun observeReports(): Flow<List<ReportEntity>> =
        reportDao.observeAll().flowOn(Dispatchers.IO)

    /**
     * Saves the report and removes the draft in a single transaction. The save is keyed
     * by the draft's UUID with REPLACE conflict strategy — retrying after a transient
     * failure (e.g. coroutine cancellation between insert + delete) cannot duplicate.
     */
    suspend fun saveReport(
        draftId: String,
        snapshot: WeatherSnapshot,
        notes: String,
        imagePath: String,
        originalImageBytes: Long,
        compressedImageBytes: Long,
    ): ReportEntity {
        val entity = ReportEntity(
            id = draftId,
            cityName = snapshot.city.name,
            country = snapshot.city.country,
            latitude = snapshot.city.latitude,
            longitude = snapshot.city.longitude,
            temperatureC = snapshot.temperatureC,
            condition = snapshot.condition,
            weatherCode = snapshot.weatherCode,
            humidity = snapshot.humidity,
            windSpeedKmh = snapshot.windSpeedKmh,
            pressureHpa = snapshot.pressureHpa,
            weatherFetchedAt = snapshot.fetchedAtEpochMillis,
            notes = notes.trim(),
            imagePath = imagePath,
            originalImageBytes = originalImageBytes,
            compressedImageBytes = compressedImageBytes,
            savedAt = System.currentTimeMillis(),
        )
        db.withTransaction {
            reportDao.insert(entity)
            draftDao.delete(draftId)
        }
        return entity
    }
}
