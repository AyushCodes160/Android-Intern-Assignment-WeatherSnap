package com.weathersnap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weathersnap.data.local.entity.DraftEntity

@Dao
interface DraftDao {

    @Query("SELECT * FROM report_drafts WHERE id = :id LIMIT 1")
    suspend fun get(id: String): DraftEntity?

    @Query("SELECT * FROM report_drafts LIMIT 1")
    suspend fun any(): DraftEntity?

    @Query("SELECT imagePath FROM report_drafts WHERE imagePath IS NOT NULL")
    suspend fun allDraftImagePaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("DELETE FROM report_drafts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM report_drafts")
    suspend fun deleteAll()
}
