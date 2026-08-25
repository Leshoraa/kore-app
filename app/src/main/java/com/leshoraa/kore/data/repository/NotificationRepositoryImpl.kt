package com.leshoraa.kore.data.repository

import com.leshoraa.kore.core.database.NotificationLogDao
import com.leshoraa.kore.core.database.NotificationLogEntity
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of NotificationRepository for auditing logs.
 */
class NotificationRepositoryImpl(private val logDao: NotificationLogDao) : NotificationRepository {
    
    override val recentEvents: Flow<List<NotificationEvent>> = logDao.getRecentLogs().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun logEvent(event: NotificationEvent) {
        logDao.insertLog(event.toEntity())
    }

    private fun NotificationLogEntity.toDomain() = NotificationEvent(
        id = id.toString(),
        packageName = packageName,
        appName = appName,
        postTimeMillis = timestamp,
        title = title,
        text = text,
        subText = null,
        isClearable = true,
        isGroupSummary = false
    )

    private fun NotificationEvent.toEntity() = NotificationLogEntity(
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        timestamp = postTimeMillis
    )
}
