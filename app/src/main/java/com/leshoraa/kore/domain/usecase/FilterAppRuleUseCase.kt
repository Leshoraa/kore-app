package com.leshoraa.kore.domain.usecase

import com.leshoraa.kore.domain.repository.RuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case to determine if a notification from a specific package should be processed.
 * Defaults to allowed if no rule is found.
 */
class FilterAppRuleUseCase(private val repository: RuleRepository) {
    suspend operator fun invoke(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val rule = repository.getRule(packageName)
        // If no rule exists, we allow it by default (or you can change to false for strict whitelist)
        rule?.isEnabled ?: true
    }
}
