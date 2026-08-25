package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.AppRule
import com.leshoraa.kore.domain.repository.RuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists an application filter rule.
 */
class SaveAppRuleUseCase(private val repository: RuleRepository) {
    suspend operator fun invoke(rule: AppRule) = withContext(Dispatchers.IO) {
        repository.saveRule(rule)
    }
}
