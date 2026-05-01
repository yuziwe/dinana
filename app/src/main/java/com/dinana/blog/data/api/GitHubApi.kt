package com.dinana.blog.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit

class GitHubApi(private val tokenProvider: () -> String) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "GitHubApi"
        private const val BASE_URL = "https://api.github.com"
    }

    private fun buildAuthRequest(url: String): Request.Builder {
        val token = tokenProvider()
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder
    }

    suspend fun listContents(owner: String, repo: String, path: String = ""): Result<List<GitHubContent>> {
        val url = if (path.isBlank()) {
            "$BASE_URL/repos/$owner/$repo/contents"
        } else {
            "$BASE_URL/repos/$owner/$repo/contents/$path"
        }
        Log.d(TAG, "listContents: owner=$owner repo=$repo path=$path")
        return runRequest(url) { response ->
            json.decodeFromString<List<GitHubContent>>(response)
        }
    }

    suspend fun getFile(owner: String, repo: String, path: String): Result<GitHubContent> {
        val url = "$BASE_URL/repos/$owner/$repo/contents/$path"
        Log.d(TAG, "getFile: owner=$owner repo=$repo path=$path")
        return runRequest(url) { response ->
            val raw = response
            // GitHub may return content array if path is a directory
            try {
                json.decodeFromString<GitHubContent>(raw)
            } catch (_: Exception) {
                // If it's a list, get the first item
                val items = json.decodeFromString<List<GitHubContent>>(raw)
                if (items.isNotEmpty()) items.first() else throw IllegalStateException("Empty directory")
            }
        }
    }

    suspend fun createOrUpdateFile(
        owner: String,
        repo: String,
        path: String,
        message: String,
        content: String,
        sha: String? = null,
        branch: String = "main"
    ): Result<CreateUpdateResponse> {
        val url = "$BASE_URL/repos/$owner/$repo/contents/$path"
        Log.d(TAG, "createOrUpdateFile: path=$path sha=$sha branch=$branch")
        val requestBody = CreateUpdateRequest(
            message = message,
            content = content,
            sha = sha,
            branch = branch
        )
        val body = json.encodeToString(CreateUpdateRequest.serializer(), requestBody)
            .toRequestBody(mediaType)

        val request = buildAuthRequest(url)
            .put(body)
            .build()

        return executeRequest(request) { response ->
            json.decodeFromString<CreateUpdateResponse>(response)
        }
    }

    suspend fun deleteFile(
        owner: String,
        repo: String,
        path: String,
        message: String,
        sha: String,
        branch: String = "main"
    ): Result<Unit> {
        val url = "$BASE_URL/repos/$owner/$repo/contents/$path"
        Log.d(TAG, "deleteFile: path=$path sha=$sha branch=$branch")
        val requestBody = DeleteRequest(
            message = message,
            sha = sha,
            branch = branch
        )
        val body = json.encodeToString(DeleteRequest.serializer(), requestBody)
            .toRequestBody(mediaType)

        val request = buildAuthRequest(url)
            .delete(body)
            .build()

        return executeRequest(request) { }
    }

    private suspend fun <T> runRequest(
        url: String,
        parser: (String) -> T
    ): Result<T> {
        return try {
            val request = buildAuthRequest(url).get().build()
            Log.d(TAG, "GET ${request.url}")
            executeRequest(request, parser)
        } catch (e: Exception) {
            Log.e(TAG, "runRequest failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Git Data API ---

    suspend fun getRef(owner: String, repo: String, branch: String): Result<GitReference> {
        val url = "$BASE_URL/repos/$owner/$repo/git/refs/heads/$branch"
        return runRequest(url) { json.decodeFromString(it) }
    }

    suspend fun getCommit(owner: String, repo: String, sha: String): Result<GitCommit> {
        val url = "$BASE_URL/repos/$owner/$repo/git/commits/$sha"
        return runRequest(url) { json.decodeFromString(it) }
    }

    suspend fun getTree(owner: String, repo: String, sha: String, recursive: Boolean = false): Result<GitTree> {
        val url = "$BASE_URL/repos/$owner/$repo/git/trees/$sha" +
            if (recursive) "?recursive=1" else ""
        return runRequest(url) { json.decodeFromString(it) }
    }

    suspend fun createTree(owner: String, repo: String, request: CreateTreeRequest): Result<GitTree> {
        val url = "$BASE_URL/repos/$owner/$repo/git/trees"
        val body = json.encodeToString(CreateTreeRequest.serializer(), request)
            .toRequestBody(mediaType)
        val httpRequest = buildAuthRequest(url)
            .post(body)
            .build()
        return executeRequest(httpRequest) { json.decodeFromString(it) }
    }

    suspend fun createCommit(owner: String, repo: String, request: CreateCommitRequest): Result<GitCommit> {
        val url = "$BASE_URL/repos/$owner/$repo/git/commits"
        val body = json.encodeToString(CreateCommitRequest.serializer(), request)
            .toRequestBody(mediaType)
        val httpRequest = buildAuthRequest(url)
            .post(body)
            .build()
        return executeRequest(httpRequest) { json.decodeFromString(it) }
    }

    suspend fun updateRef(owner: String, repo: String, ref: String, request: UpdateRefRequest): Result<Unit> {
        val url = "$BASE_URL/repos/$owner/$repo/git/refs/$ref"
        val body = json.encodeToString(UpdateRefRequest.serializer(), request)
            .toRequestBody(mediaType)
        val httpRequest = buildAuthRequest(url)
            .method("PATCH", body)
            .build()
        return executeRequest(httpRequest) { }
    }

    suspend fun createDeletionTree(
        owner: String,
        repo: String,
        baseTree: String,
        paths: List<String>
    ): Result<GitTree> {
        val url = "$BASE_URL/repos/$owner/$repo/git/trees"
        val requestBody = buildJsonObject {
            put("base_tree", baseTree)
            putJsonArray("tree") {
                paths.forEach { path ->
                    add(buildJsonObject {
                        put("path", path)
                        put("mode", JsonPrimitive("100644"))
                        put("type", JsonPrimitive("blob"))
                        put("sha", JsonNull) // null sha = delete
                    })
                }
            }
        }
        val body = requestBody.toString().toRequestBody(mediaType)
        val httpRequest = buildAuthRequest(url).post(body).build()
        return executeRequest(httpRequest) { json.decodeFromString(it) }
    }

    suspend fun createBlob(owner: String, repo: String, request: CreateBlobRequest): Result<CreateBlobResponse> {
        val url = "$BASE_URL/repos/$owner/$repo/git/blobs"
        val body = json.encodeToString(CreateBlobRequest.serializer(), request)
            .toRequestBody(mediaType)
        val httpRequest = buildAuthRequest(url)
            .post(body)
            .build()
        return executeRequest(httpRequest) { json.decodeFromString(it) }
    }

    suspend fun getLatestRelease(owner: String, repo: String): Result<GitHubRelease> {
        val url = "$BASE_URL/repos/$owner/$repo/releases/latest"
        return runRequest(url) { json.decodeFromString(it) }
    }

    private suspend fun <T> executeRequest(
        request: Request,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val snippet = body.take(200)
                Log.d(TAG, "${request.method} ${request.url} -> ${response.code} OK, body=${snippet}...")
                Result.success(parser(body))
            } else {
                val error = try {
                    json.decodeFromString<GitHubError>(body).message
                } catch (_: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                Log.w(TAG, "${request.method} ${request.url} -> ${response.code}: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "${request.method} ${request.url} failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
