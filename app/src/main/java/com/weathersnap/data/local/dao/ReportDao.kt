package com.weathersnap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weathersnap.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Query("SELECT * FROM reports ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Query("SELECT imagePath FROM reports")
    suspend fun allImagePaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity)

    @Query("SELECT COUNT(*) FROM reports WHERE id = :id")
    suspend fun exists(id: String): Int
}
