package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.model.AppRule
import com.leshoraa.kore.domain.repository.RuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists multiple application filter rules.
 */
class SaveAppRulesUseCase(private val repository: RuleRepository) {
    suspend operator fun invoke(rules: List<AppRule>) = withContext(Dispatchers.IO) {
        repository.saveRules(rules)
    }
}
