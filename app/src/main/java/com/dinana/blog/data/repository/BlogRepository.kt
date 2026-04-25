package com.dinana.blog.data.repository

import android.util.Base64
import android.util.Log
import com.dinana.blog.data.api.CreateBlobRequest
import com.dinana.blog.data.api.CreateCommitRequest
import com.dinana.blog.data.api.CreateTreeRequest
import com.dinana.blog.data.api.GitHubApi
import com.dinana.blog.data.api.GitHubContent
import com.dinana.blog.data.api.GitTreeItem
import com.dinana.blog.data.api.UpdateRefRequest
import com.dinana.blog.util.MarkdownUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.nio.charset.StandardCharsets

data class BlogPost(
    val title: String,
    val fileName: String,
    val path: String,
    val content: String = "",
    val sha: String = "",
    val size: Int = 0,
    val date: String? = null
)

class BlogRepository(private val api: GitHubApi) {

    companion object {
        private const val TAG = "BlogRepository"
        const val POSTS_DIR = "source/_posts"
    }

    suspend fun listPosts(owner: String, repo: String): Result<List<BlogPost>> {
        Log.d(TAG, "listPosts: owner=$owner repo=$repo dir=$POSTS_DIR")
        return api.listContents(owner, repo, POSTS_DIR).map { contents ->
            Log.d(TAG, "listPosts: got ${contents.size} items, filtering for .md files")
            contents
                .filter { it.type == "file" && it.name.endsWith(".md") }
                .map { it.toBlogPost() }
                .also { Log.d(TAG, "listPosts: returning ${it.size} posts") }
        }
    }

    suspend fun getPost(owner: String, repo: String, path: String): Result<BlogPost> {
        Log.d(TAG, "getPost: owner=$owner repo=$repo path=$path")
        return api.getFile(owner, repo, path).map { content ->
            val hasContent = content.content != null
            Log.d(TAG, "getPost: name=${content.name} sha=${content.sha} hasContent=$hasContent")
            val decodedContent = content.content?.let { decodeBase64(it.trim()) } ?: ""
            content.toBlogPost(content = decodedContent)
        }
    }

    suspend fun pushPost(
        owner: String,
        repo: String,
        fileName: String,
        content: String,
        existingSha: String? = null,
        branch: String = "main"
    ): Result<Unit> {
        val path = "$POSTS_DIR/$fileName"
        val encodedContent = encodeBase64(content)
        val message = if (existingSha != null) "Update $fileName" else "Create $fileName"
        Log.d(TAG, "pushPost: path=$path existingSha=$existingSha branch=$branch")

        return api.createOrUpdateFile(
            owner = owner,
            repo = repo,
            path = path,
            message = message,
            content = encodedContent,
            sha = existingSha,
            branch = branch
        ).map { }
    }

    suspend fun createAssetDir(
        owner: String,
        repo: String,
        slug: String,
        branch: String = "main"
    ): Result<Unit> {
        val path = "$POSTS_DIR/$slug/.gitkeep"
        return api.createOrUpdateFile(
            owner = owner,
            repo = repo,
            path = path,
            message = "Create asset directory for $slug",
            content = encodeBase64(""),
            branch = branch
        ).map { }
    }

    suspend fun uploadImage(
        owner: String,
        repo: String,
        slug: String,
        imageFileName: String,
        imageBytes: ByteArray,
        branch: String = "main"
    ): Result<Unit> {
        val path = "$POSTS_DIR/$slug/$imageFileName"
        return api.createOrUpdateFile(
            owner = owner,
            repo = repo,
            path = path,
            message = "Add image $imageFileName to $slug",
            content = Base64.encodeToString(imageBytes, Base64.NO_WRAP),
            branch = branch
        ).map { }
    }

    suspend fun pushPostWithImages(
        owner: String,
        repo: String,
        slug: String,
        content: String,
        images: List<Pair<String, ByteArray>>,
        existingPostSha: String? = null,
        branch: String = "main"
    ): Result<Unit> {
        Log.d(TAG, "pushPostWithImages: slug=$slug images=${images.size} existingSha=$existingPostSha")
        return try {
            // 1. Get current branch state
            val ref = api.getRef(owner, repo, branch).getOrThrow()
            val commit = api.getCommit(owner, repo, ref.`object`.sha).getOrThrow()

            // 2. Build tree items for the post file and images
            val treeItems = mutableListOf<GitTreeItem>()

            // Post markdown file (raw text content)
            treeItems.add(GitTreeItem(
                path = "$POSTS_DIR/$slug.md",
                mode = "100644",
                type = "blob",
                content = content
            ))

            // Images — create blobs first (binary), then reference by SHA
            for ((name, bytes) in images) {
                val blob = api.createBlob(owner, repo, CreateBlobRequest(
                    content = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    encoding = "base64"
                )).getOrThrow()
                treeItems.add(GitTreeItem(
                    path = "$POSTS_DIR/$slug/$name",
                    mode = "100644",
                    type = "blob",
                    sha = blob.sha
                ))
            }

            // 3. Create new tree on top of the current one
            val newTree = api.createTree(owner, repo, CreateTreeRequest(
                baseTree = commit.tree.sha,
                tree = treeItems
            )).getOrThrow()

            // 4. Create commit
            val fileDesc = if (existingPostSha != null) "Update $slug.md" else "Create $slug.md"
            val msg = if (images.isNotEmpty()) "$fileDesc + ${images.size} image(s)" else fileDesc
            val newCommit = api.createCommit(owner, repo, CreateCommitRequest(
                message = msg,
                tree = newTree.sha,
                parents = listOf(ref.`object`.sha)
            )).getOrThrow()

            // 5. Update ref
            api.updateRef(owner, repo, "heads/$branch", UpdateRefRequest(sha = newCommit.sha))
                .map { }
        } catch (e: Exception) {
            Log.e(TAG, "pushPostWithImages failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deletePostWithAssets(
        owner: String,
        repo: String,
        postPath: String,
        postSha: String,
        branch: String = "main"
    ): Result<Unit> {
        Log.d(TAG, "deletePostWithAssets: path=$postPath branch=$branch")
        return try {
            val slug = postPath.removePrefix("$POSTS_DIR/").removeSuffix(".md")
            val assetDir = "$POSTS_DIR/$slug"

            // 1. Get the current commit SHA from the branch ref
            val ref = api.getRef(owner, repo, branch).getOrThrow()
            // 2. Get the tree SHA from the commit
            val commit = api.getCommit(owner, repo, ref.`object`.sha).getOrThrow()
            // 3. Get the full tree (all files)
            val tree = api.getTree(owner, repo, commit.tree.sha, recursive = true).getOrThrow()

            // 4. Find files to delete (the post + all files in its asset dir)
            val filesToDelete = tree.tree.filter { item ->
                item.path == postPath || item.path.startsWith("$assetDir/")
            }

            // 5. Check if there are actual asset files
            val hasAssetFiles = filesToDelete.any { it.path.startsWith("$assetDir/") }

            if (!hasAssetFiles) {
                // No asset directory — use the simple Contents API delete (single commit)
                return api.deleteFile(
                    owner = owner,
                    repo = repo,
                    path = postPath,
                    message = "Delete $postPath",
                    sha = postSha,
                    branch = branch
                ).map { }
            }

            // 6. Create a new tree with the deleted files removed (sha = null = delete)
            val newTree = api.createDeletionTree(
                owner = owner,
                repo = repo,
                baseTree = commit.tree.sha,
                paths = filesToDelete.map { it.path }
            ).getOrThrow()

            // 7. Create a single commit with all deletions
            val fileNames = filesToDelete.joinToString(", ") { it.path.substringAfterLast("/") }
            val newCommit = api.createCommit(owner, repo, CreateCommitRequest(
                message = "Delete $fileNames",
                tree = newTree.sha,
                parents = listOf(ref.`object`.sha)
            )).getOrThrow()

            // 8. Update the branch ref to point to the new commit
            api.updateRef(owner, repo, "heads/$branch", UpdateRefRequest(sha = newCommit.sha))
                .map { }
        } catch (e: Exception) {
            Log.e(TAG, "deletePostWithAssets failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deletePost(
        owner: String,
        repo: String,
        path: String,
        sha: String,
        branch: String = "main"
    ): Result<Unit> {
        Log.d(TAG, "deletePost: owner=$owner repo=$repo path=$path sha=$sha")
        return api.deleteFile(
            owner = owner,
            repo = repo,
            path = path,
            message = "Delete $path",
            sha = sha,
            branch = branch
        )
    }

    private fun GitHubContent.toBlogPost(content: String = "") = BlogPost(
        title = name.removeSuffix(".md").replace("-", " "),
        fileName = name,
        path = path,
        content = content,
        sha = sha,
        size = size
    )

    private fun encodeBase64(input: String): String {
        return Base64.encodeToString(
            input.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
    }

    private fun decodeBase64(input: String): String {
        return String(Base64.decode(input, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

suspend fun enrichWithDates(owner: String, repo: String, posts: List<BlogPost>): List<BlogPost> {
        // First try extracting date from filenames (YYYY-MM-DD-title.md → 2024-01-15)
        val withFileDates = posts.map { post ->
            val dateFromName = MarkdownUtils.parseDateFromFileName(post.fileName)
            if (dateFromName != null) post.copy(date = dateFromName) else post
        }

        val needsFetch = withFileDates.filter { it.date == null }
        if (needsFetch.isEmpty()) return withFileDates

        // Only fetch content for posts that don't have a date in their filename
        return try {
            val fetched = coroutineScope {
                needsFetch.map { post ->
                    async {
                        val date = try {
                            api.getFile(owner, repo, post.path)
                                .getOrNull()
                                ?.content
                                ?.let { decodeBase64(it.trim()) }
                                ?.let { MarkdownUtils.parseDateFromFrontMatter(it) }
                        } catch (_: Exception) { null }
                        post.copy(date = date)
                    }
                }.awaitAll()
            }
            withFileDates.map { existing ->
                fetched.find { it.fileName == existing.fileName } ?: existing
            }
        } catch (_: Exception) {
            withFileDates
        }
    }
}
