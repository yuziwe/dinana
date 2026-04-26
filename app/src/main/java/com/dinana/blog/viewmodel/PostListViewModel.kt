package com.dinana.blog.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinana.blog.data.local.TokenManager
import com.dinana.blog.data.repository.BlogPost
import com.dinana.blog.data.repository.BlogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOrder { NAME, DATE_DESC, DATE_ASC }

data class PostListUiState(
    val posts: List<BlogPost> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isConfigured: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NAME
)

class PostListViewModel(
    private val repository: BlogRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "PostListViewModel"
    }

    private val _uiState = MutableStateFlow(PostListUiState())
    val uiState: StateFlow<PostListUiState> = _uiState.asStateFlow()

    init {
        checkConfiguration()
        loadPosts()
    }

    private fun checkConfiguration() {
        val configured = tokenManager.isConfigured
        Log.d(TAG, "checkConfiguration: isConfigured=$configured owner=${tokenManager.owner} repo=${tokenManager.repo} tokenPresent=${tokenManager.token.isNotBlank()}")
        _uiState.value = _uiState.value.copy(
            isConfigured = configured
        )
    }

    fun loadPosts() {
        checkConfiguration()
        if (!tokenManager.isConfigured) {
            Log.w(TAG, "loadPosts: skipped - not configured")
            return
        }

        Log.d(TAG, "loadPosts: owner=${tokenManager.owner} repo=${tokenManager.repo}")
        val isRefresh = _uiState.value.posts.isNotEmpty()

        _uiState.value = _uiState.value.copy(
            isLoading = !isRefresh,
            isRefreshing = isRefresh,
            error = null
        )

        viewModelScope.launch {
            val result = repository.listPosts(
                owner = tokenManager.owner,
                repo = tokenManager.repo
            )

            result.fold(
                onSuccess = { posts ->
                    Log.d(TAG, "loadPosts: success, ${posts.size} posts loaded")
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                    applySort()
                },
                onFailure = { e ->
                    Log.e(TAG, "loadPosts: failed: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load posts"
                    )
                }
            )
        }
    }

    fun deletePost(post: BlogPost) {
        Log.d(TAG, "deletePost: path=${post.path}")
        _uiState.value = _uiState.value.copy(isDeleting = true)
        viewModelScope.launch {
            val result = repository.deletePostWithAssets(
                owner = tokenManager.owner,
                repo = tokenManager.repo,
                postPath = post.path,
                postSha = post.sha,
                branch = tokenManager.branch
            )

            result.fold(
                onSuccess = {
                    Log.d(TAG, "deletePost: success")
                    _uiState.value = _uiState.value.copy(isDeleting = false, message = "Post deleted")
                    loadPosts()
                },
                onFailure = { e ->
                    Log.e(TAG, "deletePost: failed: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = e.message ?: "Failed to delete post"
                    )
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun toggleSort() {
        val current = _uiState.value.sortOrder
        val next = when (current) {
            SortOrder.NAME -> SortOrder.DATE_DESC
            SortOrder.DATE_DESC -> SortOrder.DATE_ASC
            SortOrder.DATE_ASC -> SortOrder.NAME
        }
        _uiState.value = _uiState.value.copy(sortOrder = next)

        if (next != SortOrder.NAME) {
            val posts = _uiState.value.posts
            if (posts.any { it.date == null }) {
                viewModelScope.launch {
                    val enriched = repository.enrichWithDates(
                        owner = tokenManager.owner,
                        repo = tokenManager.repo,
                        posts = posts
                    )
                    _uiState.value = _uiState.value.copy(
                        posts = enriched,
                        sortOrder = next
                    )
                    applySort()
                }
                return
            }
        }
        applySort()
    }

    private fun applySort() {
        val sorted = when (_uiState.value.sortOrder) {
            SortOrder.NAME -> _uiState.value.posts.sortedBy { it.fileName }
            SortOrder.DATE_DESC -> _uiState.value.posts.sortedWith { a, b ->
                when {
                    a.date == null && b.date == null -> 0
                    a.date == null -> 1
                    b.date == null -> -1
                    else -> b.date.compareTo(a.date)
                }
            }
            SortOrder.DATE_ASC -> _uiState.value.posts.sortedWith { a, b ->
                when {
                    a.date == null && b.date == null -> 0
                    a.date == null -> 1
                    b.date == null -> -1
                    else -> a.date.compareTo(b.date)
                }
            }
        }
        _uiState.value = _uiState.value.copy(posts = sorted)
    }

    fun refresh() {
        Log.d(TAG, "refresh")
        checkConfiguration()
        loadPosts()
    }
}
