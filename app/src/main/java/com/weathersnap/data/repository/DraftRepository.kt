package com.weathersnap.data.repository

import com.weathersnap.data.local.dao.DraftDao
import com.weathersnap.data.local.entity.DraftEntity
import com.weathersnap.domain.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores an in-progress report (frozen weather snapshot + notes + image) so the
 * Create Report flow survives rotation, backgrounding, or process death. Keyed by
 * a UUID created when report creation starts; that same UUID is reused on save,
 * so duplicate saves are impossible.
 */
@Singleton
class DraftRepository @Inject constructor(
    private val draftDao: DraftDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getOrCreate(
        draftId: String,
        snapshot: WeatherSnapshot,
    ): DraftEntity = withContext(Dispatchers.IO) {
        draftDao.get(draftId)?.let { return@withContext it }
        val now = System.currentTimeMillis()
        val draft = DraftEntity(
            id = draftId,
            weatherSnapshotJson = json.encodeToString(snapshot),
            notes = "",
            imagePath = null,
            originalImageBytes = 0L,
            compressedImageBytes = 0L,
            createdAt = now,
            updatedAt = now,
        )
        draftDao.upsert(draft)
        draft
    }

    suspend fun loadAny(): DraftEntity? = withContext(Dispatchers.IO) { draftDao.any() }

    suspend fun decodeSnapshot(draft: DraftEntity): WeatherSnapshot =
        json.decodeFromString(draft.weatherSnapshotJson)

    suspend fun updateNotes(draftId: String, notes: String) = withContext(Dispatchers.IO) {
        val current = draftDao.get(draftId) ?: return@withContext
        draftDao.upsert(current.copy(notes = notes, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateImage(
        draftId: String,
        imagePath: String,
        originalBytes: Long,
        compressedBytes: Long,
    ) = withContext(Dispatchers.IO) {
        val current = draftDao.get(draftId) ?: return@withContext
        // Replacing the image — delete previous if it was a draft-only file.
        current.imagePath
            ?.takeIf { it != imagePath }
            ?.let { runCatching { File(it).delete() } }
        draftDao.upsert(
            current.copy(
                imagePath = imagePath,
                originalImageBytes = originalBytes,
                compressedImageBytes = compressedBytes,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun discard(draftId: String) = withContext(Dispatchers.IO) {
        val current = draftDao.get(draftId) ?: return@withContext
        current.imagePath?.let { runCatching { File(it).delete() } }
        draftDao.delete(draftId)
    }

    suspend fun allDraftImagePaths(): List<String> = withContext(Dispatchers.IO) {
        draftDao.allDraftImagePaths()
    }
}
