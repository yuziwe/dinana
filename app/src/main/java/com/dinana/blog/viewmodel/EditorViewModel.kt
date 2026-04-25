package com.dinana.blog.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinana.blog.data.local.TokenManager
import com.dinana.blog.data.repository.BlogRepository
import com.dinana.blog.data.repository.BlogRepository.Companion.POSTS_DIR
import com.dinana.blog.util.MarkdownUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val title: String = "",
    val slug: String = "",
    val content: String = "",
    val processedContent: String = "",
    val isEditing: Boolean = false,
    val existingSha: String? = null,
    val existingPath: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class EditorViewModel(
    private val repository: BlogRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "EditorViewModel"
    }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Staged images — held in-memory until save commits everything in one batch
    private val stagedImages = mutableListOf<Pair<String, ByteArray>>()

    fun loadPost(fileName: String) {
        if (_uiState.value.isLoaded) {
            Log.d(TAG, "loadPost: already loaded, skipping")
            return
        }

        Log.d(TAG, "loadPost: fileName=$fileName")

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val result = repository.getPost(
                owner = tokenManager.owner,
                repo = tokenManager.repo,
                path = "$POSTS_DIR/$fileName"
            )

            result.fold(
                onSuccess = { post ->
                    Log.d(TAG, "loadPost: success, size=${post.size}")
                    val title = parseTitleFromFrontMatter(post.content) ?: post.fileName.removeSuffix(".md")
                    val slug = MarkdownUtils.slugify(title)
                    _uiState.value = _uiState.value.copy(
                        title = title,
                        slug = slug,
                        content = post.content,
                        processedContent = processForPreview(post.content, slug),
                        isEditing = true,
                        existingSha = post.sha,
                        existingPath = post.path,
                        isLoaded = true,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "loadPost: failed: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to load post",
                        isLoaded = true,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun initNewPost() {
        Log.d(TAG, "initNewPost")
        _uiState.value = _uiState.value.copy(isLoaded = true)
    }

    fun updateTitle(title: String) {
        val slug = MarkdownUtils.slugify(title)
        _uiState.value = _uiState.value.copy(
            title = title,
            slug = slug,
            processedContent = processForPreview(_uiState.value.content, slug)
        )
    }

    fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(
            content = content,
            processedContent = processForPreview(content, _uiState.value.slug)
        )
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Title cannot be empty")
            return
        }

        Log.d(TAG, "save: slug=${state.slug} sha=${state.existingSha} staged=${stagedImages.size}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            val sha = state.existingSha
            val slug = state.slug.ifBlank { MarkdownUtils.slugify(state.title) }
            val content = if (sha == null) {
                MarkdownUtils.generateFrontMatter(state.title) + state.content
            } else {
                state.content
            }

            val result = if (stagedImages.isEmpty()) {
                repository.pushPost(
                    owner = tokenManager.owner,
                    repo = tokenManager.repo,
                    fileName = "$slug.md",
                    content = content,
                    existingSha = sha,
                    branch = tokenManager.branch
                )
            } else {
                repository.pushPostWithImages(
                    owner = tokenManager.owner,
                    repo = tokenManager.repo,
                    slug = slug,
                    content = content,
                    images = stagedImages.toList(),
                    existingPostSha = sha,
                    branch = tokenManager.branch
                )
            }

            result.fold(
                onSuccess = {
                    Log.d(TAG, "save: pushed successfully")
                    stagedImages.clear()
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        message = "Post saved"
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "save: failed: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save post"
                    )
                }
            )
        }
    }

    fun uploadImage(imageName: String, imageBytes: ByteArray, insertAt: Int = -1) {
        Log.d(TAG, "uploadImage: name=$imageName size=${imageBytes.size}")
        stagedImages.add(imageName to imageBytes)
        val markdownRef = "![$imageName]($imageName)"
        val content = _uiState.value.content
        val pos = if (insertAt in 0..content.length) insertAt else content.length
        val updatedContent = content.substring(0, pos) + "\n$markdownRef\n" + content.substring(pos)
        updateContent(updatedContent)
        _uiState.value = _uiState.value.copy(message = "Image uploaded")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun processForPreview(content: String, slug: String): String {
        val body = MarkdownUtils.stripFrontMatter(content)
        return MarkdownUtils.resolveImageUrls(
            body,
            tokenManager.owner,
            tokenManager.repo,
            tokenManager.branch,
            slug
        )
    }

    private fun parseTitleFromFrontMatter(content: String): String? {
        if (!content.startsWith("---")) return null
        val end = content.indexOf("---", 3)
        if (end == -1) return null
        val frontMatter = content.substring(3, end)
        val titleLine = frontMatter.lines().firstOrNull { it.trimStart().startsWith("title:") } ?: return null
        return titleLine.substringAfter("title:").trim().removeSurrounding("\"")
    }
}
