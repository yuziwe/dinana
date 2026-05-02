package com.dinana.blog.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dinana.blog.data.repository.BlogPost
import com.dinana.blog.ui.theme.appBarColors
import com.dinana.blog.viewmodel.SortOrder

private enum class PostListContentState { NOT_CONFIGURED, LOADING, ERROR, EMPTY, CONTENT }

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun PostListScreen(
    posts: List<BlogPost>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isDeleting: Boolean,
    error: String?,
    message: String?,
    sortOrder: SortOrder,
    onRefresh: () -> Unit,
    onToggleSort: () -> Unit,
    onPostClick: (BlogPost) -> Unit,
    onNewPost: () -> Unit,
    onSettings: () -> Unit,
    onDeletePost: (BlogPost) -> Unit,
    onClearMessage: () -> Unit,
    isConfigured: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            onClearMessage()
        }
    }
    var showDeleteDialog by remember { mutableStateOf<BlogPost?>(null) }
    val contentState = when {
        !isConfigured -> PostListContentState.NOT_CONFIGURED
        isLoading && posts.isEmpty() -> PostListContentState.LOADING
        error != null && posts.isEmpty() -> PostListContentState.ERROR
        posts.isEmpty() -> PostListContentState.EMPTY
        else -> PostListContentState.CONTENT
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dinana")
                        AnimatedVisibility(visible = isConfigured && posts.isNotEmpty()) {
                            Text(
                                text = "${posts.size} posts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSort, enabled = isConfigured) {
                        Icon(
                            imageVector = when (sortOrder) {
                                SortOrder.NAME -> Icons.AutoMirrored.Filled.Sort
                                SortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                                SortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                            },
                            contentDescription = "Sort",
                            tint = if (sortOrder != SortOrder.NAME)
                                LocalContentColor.current
                            else
                                LocalContentColor.current.copy(alpha = 0.55f)
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = appBarColors()
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = isConfigured) {
                FloatingActionButton(onClick = onNewPost) {
                    Icon(Icons.Default.Add, contentDescription = "New Post")
                }
            }
        }
    ) { padding ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = onRefresh
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            AnimatedContent(
                targetState = contentState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith
                        fadeOut(animationSpec = tween(120))
                },
                label = "post-list-state"
            ) { state ->
                when (state) {
                    PostListContentState.NOT_CONFIGURED -> NotConfiguredMessage(onSettings = onSettings)
                    PostListContentState.LOADING -> CenteredProgress()
                    PostListContentState.ERROR -> ErrorMessage(error = error.orEmpty(), onRetry = onRefresh)
                    PostListContentState.EMPTY -> EmptyState()
                    PostListContentState.CONTENT -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(posts, key = { it.path }) { post ->
                                PostRow(
                                    post = post,
                                    onClick = { onPostClick(post) },
                                    onDelete = { showDeleteDialog = post },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    showDeleteDialog?.let { post ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete post?") },
            text = { Text("Are you sure you want to delete \"${post.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePost(post)
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isDeleting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Deleting...") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = { }
        )
    }
}

@Composable
private fun PostRow(
    post: BlogPost,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = post.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun BlogPost.subtitle(): String {
    val publishDate = date?.takeIf { it.isNotBlank() }?.take(10)
    return if (publishDate != null) {
        "$publishDate  -  $fileName"
    } else {
        fileName
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotConfiguredMessage(onSettings: () -> Unit) {
    EmptyPanel(
        title = "Welcome to Dinana",
        body = "Connect your GitHub blog repository to start writing.",
        action = {
            Button(onClick = onSettings) {
                Text("Open Settings")
            }
        }
    )
}

@Composable
private fun EmptyState() {
    EmptyPanel(
        title = "No posts yet",
        body = "Use the new post button when you are ready to publish."
    )
}

@Composable
private fun ErrorMessage(error: String, onRetry: () -> Unit) {
    EmptyPanel(
        title = "Could not load posts",
        body = error,
        action = {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        },
        isError = true
    )
}

@Composable
private fun EmptyPanel(
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
    isError: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        action?.let {
            Spacer(modifier = Modifier.height(22.dp))
            it()
        }
    }
}
