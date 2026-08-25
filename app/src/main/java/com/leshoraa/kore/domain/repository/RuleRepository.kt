package com.leshoraa.kore.domain.repository

import com.leshoraa.kore.domain.model.AppRule
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing application rules.
 */
interface RuleRepository {
    fun getAllRules(): Flow<List<AppRule>>
    suspend fun getRule(packageName: String): AppRule?
    suspend fun saveRule(rule: AppRule)
    suspend fun deleteRule(packageName: String)
}
