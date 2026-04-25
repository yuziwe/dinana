package com.dinana.blog.navigation

object Routes {
    const val POST_LIST = "post_list"
    const val EDITOR = "editor/{fileName}"
    const val EDITOR_NEW = "editor_new"
    const val SETTINGS = "settings"

    fun editorRoute(fileName: String? = null): String {
        return if (fileName != null) "editor/$fileName" else EDITOR_NEW
    }
}
