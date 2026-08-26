package com.leshoraa.kore.data.repository

import com.leshoraa.kore.core.database.AppRuleEntity
import com.leshoraa.kore.core.database.RuleDao
import com.leshoraa.kore.domain.model.AppRule
import com.leshoraa.kore.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of RuleRepository.
 */
class RuleRepositoryImpl(private val ruleDao: RuleDao) : RuleRepository {

    override fun getAllRules(): Flow<List<AppRule>> {
        return ruleDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRule(packageName: String): AppRule? {
        return ruleDao.getRule(packageName)?.toDomain()
    }

    override suspend fun saveRule(rule: AppRule) {
        ruleDao.insertRule(rule.toEntity())
    }

    override suspend fun saveRules(rules: List<AppRule>) {
        ruleDao.insertRules(rules.map { it.toEntity() })
    }

    override suspend fun deleteRule(packageName: String) {
        ruleDao.deleteRule(packageName)
    }

    private fun AppRuleEntity.toDomain() = AppRule(
        packageName = packageName,
        appName = appName,
        isEnabled = isEnabled
    )

    private fun AppRule.toEntity() = AppRuleEntity(
        packageName = packageName,
        appName = appName,
        isEnabled = isEnabled
    )
}
