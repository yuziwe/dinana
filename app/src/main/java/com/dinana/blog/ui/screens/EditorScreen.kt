package com.dinana.blog.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dinana.blog.ui.components.MarkdownPreview
import com.dinana.blog.ui.theme.appBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class EditorMode { EDIT, PREVIEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    title: String,
    content: String,
    processedContent: String,
    slug: String,
    isEditing: Boolean,
    isLoading: Boolean,
    isSaving: Boolean,
    error: String?,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onImagePicked: (name: String, bytes: ByteArray, cursorPos: Int) -> Unit,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(EditorMode.EDIT) }
    var contentFieldValue by remember { mutableStateOf(TextFieldValue(content)) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sync ViewModel content changes (e.g. image upload) back to local field value
    LaunchedEffect(content) {
        if (contentFieldValue.text != content) {
            val newPos = contentFieldValue.selection.start.coerceAtMost(content.length)
            contentFieldValue = TextFieldValue(content, TextRange(newPos))
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cursorPos = contentFieldValue.selection.start
            scope.launch(Dispatchers.IO) {
                val name = resolveImageName(context, uri) ?: "image_${System.currentTimeMillis()}.png"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    onImagePicked(name, bytes, cursorPos)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "Edit Post" else "New Post")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = !isSaving && title.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = "Push to GitHub")
                        }
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
            // Post title
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Post Title") },
                placeholder = { Text("My Awesome Post") },
                supportingText = {
                    Text(
                        text = if (slug.isNotBlank()) "Filename: ${slug}.md → source/_posts/"
                        else "Enter a title to generate the filename"
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Edit / Preview toggle + Image button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == EditorMode.EDIT,
                    onClick = { mode = EditorMode.EDIT },
                    label = { Text("Edit") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = mode == EditorMode.PREVIEW,
                    onClick = { mode = EditorMode.PREVIEW },
                    label = { Text("Preview") }
                )
                Spacer(modifier = Modifier.weight(1f))
                if (mode == EditorMode.EDIT && slug.isNotBlank()) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Insert Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content area
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else when (mode) {
                EditorMode.EDIT -> {
                    OutlinedTextField(
                        value = contentFieldValue,
                        onValueChange = { newValue ->
                            contentFieldValue = newValue
                            onContentChange(newValue.text)
                        },
                        placeholder = { Text("Write your markdown here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        enabled = !isSaving
                    )
                }
                EditorMode.PREVIEW -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (content.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Nothing to preview",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            MarkdownPreview(
                                markdown = processedContent,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Error message
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (isSaving) {
        AlertDialog(
            onDismissRequest = { }, // non-cancellable
            title = { Text("Saving...") },
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

private fun resolveImageName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) it.getString(nameIndex) else null
        } else null
    }
}
