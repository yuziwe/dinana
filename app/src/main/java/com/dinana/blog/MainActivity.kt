package com.dinana.blog

import android.os.Bundle
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.dinana.blog.ui.theme.DinanaTheme
import com.dinana.blog.viewmodel.*

// Simple ViewModel factory for manual DI
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
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
                container.tokenManager
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as DinanaApp).container
        val factory = ViewModelFactory(container)

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

    NavHost(navController = navController, startDestination = Routes.POST_LIST) {
        composable(Routes.POST_LIST) {
            val vm: PostListViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()

            // Refresh posts when screen becomes visible (e.g. after saving a new post)
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
                onTokenChange = { vm.updateToken(it) },
                onOwnerChange = { vm.updateOwner(it) },
                onRepoChange = { vm.updateRepo(it) },
                onBranchChange = { vm.updateBranch(it) },
                onSave = {
                    vm.saveSettings()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
