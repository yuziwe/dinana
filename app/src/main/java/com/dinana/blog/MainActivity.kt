package com.dinana.blog

import android.os.Bundle
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dinana.blog.navigation.Routes
import com.dinana.blog.ui.screens.EditorScreen
import com.dinana.blog.ui.screens.PostListScreen
import com.dinana.blog.ui.screens.SettingsScreen
import com.dinana.blog.data.api.GitHubApi
import com.dinana.blog.data.api.GitHubRelease
import com.dinana.blog.ui.theme.DinanaTheme
import com.dinana.blog.util.UpdateUtils
import kotlinx.coroutines.launch
import com.dinana.blog.viewmodel.*

private const val REFRESH_POSTS_KEY = "refresh_posts"

// Simple ViewModel factory for manual DI
class ViewModelFactory(
    private val container: AppContainer,
    private val currentVersion: String,
    private val appOwner: String,
    private val appRepo: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass == PostListViewModel::class.java -> PostListViewModel(
                container.provideRepository(),
                container.tokenManager
            ) as T
            modelClass == EditorViewModel::class.java -> EditorViewModel(
                container.provideRepository(),
                container.tokenManager
            ) as T
            modelClass == SettingsViewModel::class.java -> SettingsViewModel(
                container.tokenManager,
                GitHubApi { container.tokenManager.token },
                currentVersion,
                appOwner,
                appRepo
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as DinanaApp).container
        val factory = ViewModelFactory(
            container,
            BuildConfig.VERSION_NAME,
            getString(R.string.dinana_github_owner),
            getString(R.string.dinana_github_repo)
        )

        setContent {
            DinanaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DinanaNavHost(factory)
                }
            }
        }
    }
}

@Composable
fun DinanaNavHost(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Startup update check (runs once on app launch)
    val app = context.applicationContext as DinanaApp
    val appOwner = context.getString(R.string.dinana_github_owner)
    val appRepo = context.getString(R.string.dinana_github_repo)
    var updateAvailable by remember { mutableStateOf<GitHubRelease?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val api = GitHubApi { app.container.tokenManager.token }
        val result = api.getLatestRelease(appOwner, appRepo)
        result.onSuccess { release ->
            if (isNewerVersion(release.tagName, BuildConfig.VERSION_NAME)) {
                updateAvailable = release
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.POST_LIST) {
        composable(Routes.POST_LIST) { backStackEntry ->
            val vm: PostListViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val shouldRefreshPosts by backStackEntry.savedStateHandle
                .getStateFlow(REFRESH_POSTS_KEY, false)
                .collectAsStateWithLifecycle()

            LaunchedEffect(shouldRefreshPosts) {
                if (shouldRefreshPosts) {
                    backStackEntry.savedStateHandle[REFRESH_POSTS_KEY] = false
                    vm.loadPosts()
                }
            }

            PostListScreen(
                posts = state.posts,
                isLoading = state.isLoading,
                isRefreshing = state.isRefreshing,
                isDeleting = state.isDeleting,
                error = state.error,
                message = state.message,
                sortOrder = state.sortOrder,
                onRefresh = { vm.refresh() },
                onToggleSort = { vm.toggleSort() },
                onPostClick = { post ->
                    navController.navigate(Routes.editorRoute(post.fileName))
                },
                onNewPost = {
                    navController.navigate(Routes.editorRoute())
                },
                onSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onDeletePost = { post -> vm.deletePost(post) },
                onClearMessage = { vm.clearMessage() },
                isConfigured = state.isConfigured
            )
        }

        composable(Routes.EDITOR_NEW) {
            val vm: EditorViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            LaunchedEffect(Unit) { vm.initNewPost() }
            LaunchedEffect(state.message) {
                state.message?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    vm.clearMessage()
                    if (msg == "Post saved") {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(REFRESH_POSTS_KEY, true)
                        navController.popBackStack()
                    }
                }
            }

            EditorScreen(
                title = state.title,
                content = state.content,
                processedContent = state.processedContent,
                slug = state.slug,
                isEditing = state.isEditing,
                isLoading = state.isLoading,
                isSaving = state.isSaving,
                error = state.error,
                onTitleChange = { vm.updateTitle(it) },
                onContentChange = { vm.updateContent(it) },
                onSave = { vm.save() },
                onImagePicked = { name, bytes, pos -> vm.uploadImage(name, bytes, pos) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("fileName") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileName = backStackEntry.arguments?.getString("fileName") ?: return@composable
            val vm: EditorViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            LaunchedEffect(fileName) { vm.loadPost(fileName) }
            LaunchedEffect(state.message) {
                state.message?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    vm.clearMessage()
                    if (msg == "Post saved") {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(REFRESH_POSTS_KEY, true)
                        navController.popBackStack()
                    }
                }
            }

            EditorScreen(
                title = state.title,
                content = state.content,
                processedContent = state.processedContent,
                slug = state.slug,
                isEditing = state.isEditing,
                isLoading = state.isLoading,
                isSaving = state.isSaving,
                error = state.error,
                onTitleChange = { vm.updateTitle(it) },
                onContentChange = { vm.updateContent(it) },
                onSave = { vm.save() },
                onImagePicked = { name, bytes, pos -> vm.uploadImage(name, bytes, pos) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) { vm.loadSettings() }

            SettingsScreen(
                token = state.token,
                owner = state.owner,
                repo = state.repo,
                branch = state.branch,
                isConfigured = state.isConfigured,
                isCheckingUpdate = state.isCheckingUpdate,
                updateAvailable = state.updateAvailable,
                isUpToDate = state.isUpToDate,
                appVersion = state.appVersion,
                onTokenChange = { vm.updateToken(it) },
                onOwnerChange = { vm.updateOwner(it) },
                onRepoChange = { vm.updateRepo(it) },
                onBranchChange = { vm.updateBranch(it) },
                onSave = {
                    if (vm.saveSettings()) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(REFRESH_POSTS_KEY, true)
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                onCheckUpdate = { vm.checkForUpdate() },
                onDismissUpdate = { vm.dismissUpdate() },
                onClearUpToDate = { vm.clearUpToDate() },
            )
        }
    }

    // Update dialog (shown on top of any screen)
    updateAvailable?.let { release ->
        AlertDialog(
            onDismissRequest = { if (!isDownloading) { updateAvailable = null; downloadError = null } },
            title = { Text("Update Available ${release.tagName}") },
            text = {
                Column {
                    Text("A new version of Dinana is available.")
                    if (!release.body.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = release.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Downloading...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    downloadError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    TextButton(onClick = {
                        isDownloading = true
                        downloadError = null
                        scope.launch {
                            try {
                                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                                if (apkAsset != null) {
                                    UpdateUtils.downloadAndInstallApk(
                                        context, apkAsset.browserDownloadUrl, release.tagName
                                    )
                                    updateAvailable = null
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                                    context.startActivity(intent)
                                    updateAvailable = null
                                }
                            } catch (e: Exception) {
                                downloadError = "Download failed: ${e.message}"
                                isDownloading = false
                            }
                        }
                    }) {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { updateAvailable = null; downloadError = null }) {
                        Text("Later")
                    }
                }
            }
        )
    }
}

private fun isNewerVersion(latestTag: String, current: String): Boolean {
    val latest = latestTag.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(latest.size, currentParts.size)) {
        val l = latest.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l != c) return l > c
    }
    return false
}
