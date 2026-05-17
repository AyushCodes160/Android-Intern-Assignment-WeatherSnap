package com.weathersnap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton draft row (id is the draft UUID assigned when report creation starts).
 * Persists the in-progress report across rotation / process death so we can recover
 * without creating duplicate reports.
 */
@Entity(tableName = "report_drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val weatherSnapshotJson: String,
    val notes: String,
    val imagePath: String?,
    val originalImageBytes: Long,
    val compressedImageBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
