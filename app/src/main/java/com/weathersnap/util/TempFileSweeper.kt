package com.weathersnap.util

import android.content.Context
import com.weathersnap.data.local.dao.DraftDao
import com.weathersnap.data.local.dao.ReportDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On app start, removes any capture/compressed files in cacheDir/captures that are not
 * referenced by a saved report or an in-progress draft. This guarantees temp images
 * don't leak indefinitely even if the process was killed before save/discard ran.
 */
@Singleton
class TempFileSweeper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportDao: ReportDao,
    private val draftDao: DraftDao,
) {
    suspend fun sweep() = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, ImageCompressor.CAPTURES_DIR)
        if (!dir.exists()) return@withContext

        val referenced: Set<String> = buildSet {
            addAll(reportDao.allImagePaths())
            addAll(draftDao.allDraftImagePaths())
        }
        dir.listFiles()?.forEach { file ->
            if (file.absolutePath !in referenced) runCatching { file.delete() }
        }
    }
}
