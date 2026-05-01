package com.dinana.blog.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinana.blog.data.api.GitHubApi
import com.dinana.blog.data.api.GitHubRelease
import com.dinana.blog.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val token: String = "",
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val isConfigured: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val updateAvailable: GitHubRelease? = null,
    val isUpToDate: Boolean = false,
    val appVersion: String = "",
)

class SettingsViewModel(
    private val tokenManager: TokenManager,
    private val api: GitHubApi,
    private val currentVersion: String,
    private val appOwner: String,
    private val appRepo: String,
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadSettings() {
        _uiState.value = SettingsUiState(
            token = tokenManager.token,
            owner = tokenManager.owner,
            repo = tokenManager.repo,
            branch = tokenManager.branch,
            isConfigured = tokenManager.isConfigured,
            appVersion = currentVersion,
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

    fun checkForUpdate() {
        if (appOwner.isBlank() || appRepo.isBlank()) return

        _uiState.value = _uiState.value.copy(isCheckingUpdate = true)

        viewModelScope.launch {
            val result = api.getLatestRelease(appOwner, appRepo)
            result.fold(
                onSuccess = { release ->
                    if (isNewerVersion(release.tagName, currentVersion)) {
                        _uiState.value = _uiState.value.copy(
                            isCheckingUpdate = false,
                            updateAvailable = release,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCheckingUpdate = false,
                            isUpToDate = true,
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "checkForUpdate: failed: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isCheckingUpdate = false,
                    )
                }
            )
        }
    }

    fun dismissUpdate() {
        _uiState.value = _uiState.value.copy(updateAvailable = null)
    }

    fun clearUpToDate() {
        _uiState.value = _uiState.value.copy(isUpToDate = false)
    }

    private fun isNewerVersion(latestTag: String, current: String): Boolean {
        val latest = latestTag.removePrefix("v")
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }
}
