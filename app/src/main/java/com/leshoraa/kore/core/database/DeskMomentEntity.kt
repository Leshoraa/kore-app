package com.leshoraa.kore.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved desk moment.
 */
@Entity(tableName = "desk_moments")
data class DeskMomentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "expression_name")
    val expressionName: String = "IDLE",

    @ColumnInfo(name = "valence")
    val valence: Float = 0f,

    @ColumnInfo(name = "arousal")
    val arousal: Float = 0f,

    @ColumnInfo(name = "note")
    val note: String = ""
)
