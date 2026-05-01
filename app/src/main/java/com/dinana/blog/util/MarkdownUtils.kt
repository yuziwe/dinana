package com.dinana.blog.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MarkdownUtils {

    fun slugify(title: String): String {
        return title.trim()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5\\-]"), "")
            .trim('-')
            .ifEmpty { "untitled" }
    }

    fun generateFrontMatter(title: String): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return """---
title: $title
date: $now
tags:
---

"""
    }

    fun stripFrontMatter(markdown: String): String {
        val trimmed = markdown.trimStart()
        if (!trimmed.startsWith("---")) return trimmed

        val endIndex = trimmed.indexOf("---", 3)
        return if (endIndex != -1) {
            trimmed.substring(endIndex + 3).trimStart()
        } else {
            trimmed
        }
    }

    fun parseDateFromFileName(fileName: String): String? {
        val regex = Regex("""^(\d{4}-\d{2}-\d{2})-""")
        return regex.find(fileName)?.groupValues?.get(1)
    }

    fun parseDateFromFrontMatter(content: String): String? {
        if (!content.startsWith("---")) return null
        val end = content.indexOf("---", 3)
        if (end == -1) return null
        val frontMatter = content.substring(3, end)
        val dateLine = frontMatter.lines().firstOrNull { it.trimStart().startsWith("date:") } ?: return null
        return dateLine.substringAfter("date:").trim()
    }

    fun resolveImageUrls(markdown: String, owner: String, repo: String, branch: String, slug: String): String {
        val baseUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/source/_posts/$slug"
        return markdown.replace(Regex("""!\[([^\]]*)]\(([^)]+)\)""")) { match ->
            val alt = match.groupValues[1]
            val url = match.groupValues[2].trim()
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("data:")) {
                val cleanUrl = url.removePrefix("./")
                "![$alt]($baseUrl/$cleanUrl)"
            } else {
                match.value
            }
        }
    }
}
