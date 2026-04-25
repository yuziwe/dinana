package com.dinana.blog.ui.components

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.image.glide.GlideImagesPlugin

@Composable
fun MarkdownPreview(markdown: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(GlideImagesPlugin.create(context))
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx, null, android.R.attr.textViewStyle).also { tv ->
                tv.textSize = 14f
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        }
    )
}
