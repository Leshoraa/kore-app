package com.leshoraa.kore.presentation.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leshoraa.kore.domain.model.AppRule
import com.leshoraa.kore.domain.usecase.GetInstalledAppsUseCase
import com.leshoraa.kore.domain.usecase.SaveAppRuleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the application filter rules screen.
 */
class RulesViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val saveAppRuleUseCase: SaveAppRuleUseCase
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppRule>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _apps.value = getInstalledAppsUseCase()
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppRule(appRule: AppRule) {
        viewModelScope.launch {
            val updatedRule = appRule.copy(isEnabled = !appRule.isEnabled)
            saveAppRuleUseCase(updatedRule)
            // Update local state for immediate UI feedback
            _apps.value = _apps.value.map { if (it.packageName == appRule.packageName) updatedRule else it }
        }
    }
}
