package com.dinana.blog.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dinana.blog.data.api.GitHubRelease
import com.dinana.blog.ui.theme.appBarColors
import com.dinana.blog.util.UpdateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    token: String,
    owner: String,
    repo: String,
    branch: String,
    onTokenChange: (String) -> Unit,
    onOwnerChange: (String) -> Unit,
    onRepoChange: (String) -> Unit,
    onBranchChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    isConfigured: Boolean,
    isCheckingUpdate: Boolean = false,
    updateAvailable: GitHubRelease? = null,
    isUpToDate: Boolean = false,
    appVersion: String = "",
    onCheckUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onClearUpToDate: () -> Unit = {},
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isUpToDate) {
        if (isUpToDate) {
            Toast.makeText(context, "You're up to date", Toast.LENGTH_SHORT).show()
            onClearUpToDate()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = appBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "GitHub Configuration",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A GitHub Personal Access Token with repo scope is required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text("Personal Access Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = owner,
                onValueChange = onOwnerChange,
                label = { Text("Repository Owner") },
                placeholder = { Text("e.g. owner") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = repo,
                onValueChange = onRepoChange,
                label = { Text("Repository Name") },
                placeholder = { Text("e.g. repo-name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = branch,
                onValueChange = onBranchChange,
                label = { Text("Branch") },
                placeholder = { Text("main") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()
            ) {
                Text("Save")
            }

            OutlinedButton(
                onClick = onCheckUpdate,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCheckingUpdate
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text("Check for Updates")
            }

            if (appVersion.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "App version: $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    updateAvailable?.let { release ->
        AlertDialog(
            onDismissRequest = { if (!isDownloading) onDismissUpdate() },
            title = { Text("Update Available ${release.tagName}") },
            text = {
                Column {
                    Text("A new version is available.")
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
                        scope.launch {
                            try {
                                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                                if (apkAsset != null) {
                                    UpdateUtils.downloadAndInstallApk(
                                        context, apkAsset.browserDownloadUrl, release.tagName
                                    )
                                    onDismissUpdate()
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                                    context.startActivity(intent)
                                    onDismissUpdate()
                                }
                            } catch (e: Exception) {
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
                    TextButton(onClick = onDismissUpdate) {
                        Text("Later")
                    }
                }
            }
        )
    }
}
