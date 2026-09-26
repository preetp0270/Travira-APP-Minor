package com.example.travira.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists access + refresh tokens, user info, and app preferences
 * (theme + local notification preference mirror).
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    /** "system" | "light" | "dark" */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF, value).apply()

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank()

    val isAdmin: Boolean
        get() {
            val r = userRole ?: return false
            return r == "admin" || r == "superadmin"
        }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        name: String,
        email: String,
        role: String = "user"
    ) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun clear() {
        // Single atomic edit — avoid clear()+set race that can drop prefs mid-write
        val theme = themeMode
        val notif = notificationsEnabled
        prefs.edit()
            .clear()
            .putString(KEY_THEME, theme)
            .putBoolean(KEY_NOTIF, notif)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "travira_session"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_NOTIF = "notifications_enabled"
    }
}
