package com.dinana.blog

import android.app.Application
import com.dinana.blog.data.api.GitHubApi
import com.dinana.blog.data.local.TokenManager
import com.dinana.blog.data.repository.BlogRepository

class AppContainer(context: Application) {
    val tokenManager = TokenManager(context)

    fun provideRepository(): BlogRepository {
        return BlogRepository(GitHubApi { tokenManager.token })
    }
}

class DinanaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
