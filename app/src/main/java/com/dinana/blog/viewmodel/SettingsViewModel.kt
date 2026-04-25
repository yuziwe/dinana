package com.dinana.blog.viewmodel

import androidx.lifecycle.ViewModel
import com.dinana.blog.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val token: String = "",
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val isConfigured: Boolean = false
)

class SettingsViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadSettings() {
        _uiState.value = SettingsUiState(
            token = tokenManager.token,
            owner = tokenManager.owner,
            repo = tokenManager.repo,
            branch = tokenManager.branch,
            isConfigured = tokenManager.isConfigured
        )
    }

    fun updateToken(token: String) {
        _uiState.value = _uiState.value.copy(token = token)
    }

    fun updateOwner(owner: String) {
        _uiState.value = _uiState.value.copy(owner = owner)
    }

    fun updateRepo(repo: String) {
        _uiState.value = _uiState.value.copy(repo = repo)
    }

    fun updateBranch(branch: String) {
        _uiState.value = _uiState.value.copy(branch = branch)
    }

    fun saveSettings() {
        val state = _uiState.value
        tokenManager.token = state.token
        tokenManager.owner = state.owner
        tokenManager.repo = state.repo
        tokenManager.branch = state.branch
        _uiState.value = state.copy(isConfigured = true)
    }
}
