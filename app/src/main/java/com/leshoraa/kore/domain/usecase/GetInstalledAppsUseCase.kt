package com.leshoraa.kore.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.leshoraa.kore.domain.model.AppRule
import com.leshoraa.kore.domain.repository.RuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Retrieves a list of all installed user applications, merged with their current filter status.
 */
class GetInstalledAppsUseCase(
    private val context: Context,
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(): List<AppRule> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val existingRules = ruleRepository.getAllRules().first().associateBy { it.packageName }

        installedApps
            .filter { appInfo ->
                // Only show apps that can be launched (user-facing apps)
                // This will include WhatsApp and exclude background system services.
                pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .map { appInfo ->
                val packageName = appInfo.packageName
                val appName = pm.getApplicationLabel(appInfo).toString()
                val isEnabled = existingRules[packageName]?.isEnabled ?: true
                AppRule(packageName, appName, isEnabled)
            }
            .sortedBy { it.appName }
    }
}
