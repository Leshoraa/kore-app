package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.DeskMoment
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing desk moments and snapshots.
 */
interface MomentRepository {
    val momentsFlow: Flow<List<DeskMoment>>

    suspend fun captureAndSaveMoment(host: String): Result<DeskMoment>
    suspend fun deleteMoment(id: Long, filePath: String): Result<Unit>
    suspend fun clearAllMoments(): Result<Unit>
    suspend fun updateMomentNote(id: Long, note: String): Result<Unit>
}
