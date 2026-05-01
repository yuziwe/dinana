package com.dinana.blog.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun downloadAndInstallApk(context: Context, downloadUrl: String, tagName: String) {
        val fileName = "dinana-${tagName.removePrefix("v")}.apk"
        val file = File(context.cacheDir, fileName)

        val response = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Dinana")
                .get()
                .build()
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            throw Exception("Download failed: HTTP ${response.code}")
        }

        val contentLength = response.body?.contentLength() ?: -1L
        withContext(Dispatchers.IO) {
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Empty response body")
        }

        if (contentLength > 0 && file.length() != contentLength) {
            file.delete()
            throw Exception("Download incomplete: ${file.length()}/$contentLength bytes")
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
