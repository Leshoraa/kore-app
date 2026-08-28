package com.leshoraa.kore.data.repository

import android.content.Context
import android.util.Log
import com.leshoraa.kore.core.database.DeskMomentDao
import com.leshoraa.kore.core.database.DeskMomentEntity
import com.leshoraa.kore.data.remote.MjpegStreamDecoder
import com.leshoraa.kore.domain.model.DeskMoment
import com.leshoraa.kore.domain.repository.MomentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Concrete implementation of MomentRepository.
 */
class MomentRepositoryImpl(
    private val context: Context,
    private val deskMomentDao: DeskMomentDao
) : MomentRepository {

    companion object {
        private const val TAG = "MomentRepo"
        private const val TIMEOUT_MS = 10000
    }

    override val momentsFlow: Flow<List<DeskMoment>> =
        deskMomentDao.getAllMomentsFlow().map { list ->
            list.map { entity ->
                DeskMoment(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    filePath = entity.filePath,
                    expressionName = entity.expressionName,
                    valence = entity.valence,
                    arousal = entity.arousal,
                    note = entity.note
                )
            }
        }

    override suspend fun captureAndSaveMoment(host: String): Result<DeskMoment> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val sanitized = MjpegStreamDecoder.sanitizeHost(host)
            val endpoint = "http://$sanitized:80/snapshot"
            Log.d(TAG, "Fetching moment snapshot from $endpoint")

            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("HTTP Error $responseCode fetching snapshot")
                }

                val valenceHdr = connection.getHeaderField("X-Moment-Valence")?.toFloatOrNull() ?: 0f
                val arousalHdr = connection.getHeaderField("X-Moment-Arousal")?.toFloatOrNull() ?: 0f
                val exprHdr = connection.getHeaderField("X-Moment-Expression") ?: "IDLE"

                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) {
                    throw IllegalStateException("Received empty snapshot bytes")
                }

                // Compress on phone CPU and generate fast thumbnail
                val momentsDir = File(context.filesDir, "moments")
                val timestamp = System.currentTimeMillis()
                val (fullPath, _) = com.leshoraa.kore.core.common.ImageOptimizer.compressAndSaveMoment(
                    rawBytes = bytes,
                    destinationDir = momentsDir,
                    timestamp = timestamp
                )

                val entity = DeskMomentEntity(
                    timestamp = timestamp,
                    filePath = fullPath,
                    expressionName = exprHdr,
                    valence = valenceHdr,
                    arousal = arousalHdr,
                    note = ""
                )

                val generatedId = deskMomentDao.insertMoment(entity)
                Log.i(TAG, "Saved compressed desk moment ID: $generatedId, path: $fullPath")

                DeskMoment(
                    id = generatedId,
                    timestamp = timestamp,
                    filePath = fullPath,
                    expressionName = exprHdr,
                    valence = valenceHdr,
                    arousal = arousalHdr,
                    note = ""
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun deleteMoment(id: Long, filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            deskMomentDao.deleteMomentById(id)
            com.leshoraa.kore.core.common.ImageOptimizer.evictFromCache(filePath)
            try {
                val file = File(filePath)
                if (file.exists()) file.delete()
                val thumbFile = File(filePath.replace(".jpg", "_thumb.jpg"))
                if (thumbFile.exists()) thumbFile.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete snapshot file $filePath: ${e.message}")
            }
            Unit
        }
    }

    override suspend fun clearAllMoments(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            deskMomentDao.clearAllMoments()
            com.leshoraa.kore.core.common.ImageOptimizer.clearCache()
            val momentsDir = File(context.filesDir, "moments")
            if (momentsDir.exists()) {
                momentsDir.deleteRecursively()
                momentsDir.mkdirs()
            }
            Unit
        }
    }

    override suspend fun updateMomentNote(id: Long, note: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val existing = deskMomentDao.getMomentById(id) ?: return@runCatching
            deskMomentDao.insertMoment(existing.copy(note = note))
            Unit
        }
    }
}
