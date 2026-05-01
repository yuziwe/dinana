package com.dinana.blog.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubContent(
    val name: String,
    val path: String,
    val sha: String,
    val type: String,
    @SerialName("download_url") val downloadUrl: String? = null,
    val content: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val size: Int = 0
)

@Serializable
data class CreateUpdateRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String = "main"
)

@Serializable
data class CreateUpdateResponse(
    val content: GitHubContent? = null,
    val commit: CommitInfo? = null
)

@Serializable
data class CommitInfo(
    val sha: String,
    val message: String? = null
)

@Serializable
data class DeleteRequest(
    val message: String,
    val sha: String,
    val branch: String = "main"
)

@Serializable
data class GitHubError(
    val message: String,
    val documentationUrl: String? = null
)

// Git Data API models

@Serializable
data class GitReference(
    val ref: String = "",
    val `object`: GitRefObject = GitRefObject()
)

@Serializable
data class GitRefObject(
    val sha: String = "",
    val type: String = "",
    val url: String = ""
)

@Serializable
data class GitCommit(
    val sha: String = "",
    val tree: GitTreeInfo = GitTreeInfo(),
    val message: String = ""
)

@Serializable
data class GitTreeInfo(
    val sha: String = "",
    val url: String = ""
)

@Serializable
data class GitTree(
    val sha: String = "",
    val tree: List<GitTreeItem> = emptyList(),
    val truncated: Boolean = false
)

@Serializable
data class GitTreeItem(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String? = null,
    val content: String? = null
)

@Serializable
data class CreateBlobRequest(
    val content: String,
    val encoding: String
)

@Serializable
data class CreateBlobResponse(
    val sha: String
)

@Serializable
data class CreateTreeRequest(
    @SerialName("base_tree") val baseTree: String,
    val tree: List<GitTreeItem>
)

@Serializable
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>
)

@Serializable
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = false
)

@Serializable
data class ReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("content_type") val contentType: String? = null,
    val size: Long = 0,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("body") val body: String? = null,
    val assets: List<ReleaseAsset> = emptyList(),
)
