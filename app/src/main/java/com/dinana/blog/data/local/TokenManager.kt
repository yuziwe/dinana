package com.dinana.blog.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var owner: String
        get() = prefs.getString(KEY_OWNER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OWNER, value).apply()

    var repo: String
        get() = prefs.getString(KEY_REPO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REPO, value).apply()

    var branch: String
        get() = prefs.getString(KEY_BRANCH, "main") ?: "main"
        set(value) = prefs.edit().putString(KEY_BRANCH, value).apply()

    val isConfigured: Boolean
        get() = token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "dinana_secure_prefs"
        private const val KEY_TOKEN = "github_token"
        private const val KEY_OWNER = "github_owner"
        private const val KEY_REPO = "github_repo"
        private const val KEY_BRANCH = "github_branch"
    }
}
