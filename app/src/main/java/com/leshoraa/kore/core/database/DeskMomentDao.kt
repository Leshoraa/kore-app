package com.leshoraa.kore.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Desk Moments.
 */
@Dao
interface DeskMomentDao {

    @Query("SELECT * FROM desk_moments ORDER BY timestamp DESC")
    fun getAllMomentsFlow(): Flow<List<DeskMomentEntity>>

    @Query("SELECT * FROM desk_moments WHERE id = :id LIMIT 1")
    suspend fun getMomentById(id: Long): DeskMomentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: DeskMomentEntity): Long

    @Query("DELETE FROM desk_moments WHERE id = :id")
    suspend fun deleteMomentById(id: Long)

    @Query("DELETE FROM desk_moments")
    suspend fun clearAllMoments()
}
