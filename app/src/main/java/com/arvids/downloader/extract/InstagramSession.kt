package com.arvids.downloader.extract

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

/**
 * Instagram login session, stored privately on this phone only.
 * The user logs in with their own Instagram account inside the app's
 * WebView; the password is typed on Instagram's own login page and is
 * never seen or stored by the app. Only the session cookies are kept,
 * so the extractor can fetch reels as the logged-in user.
 */
object InstagramSession {
    private const val PREFS = "ar_vids_ig_session"
    private const val KEY_SESSION_ID = "sessionid"
    private const val KEY_CSRF = "csrftoken"
    private const val KEY_USER_ID = "ds_user_id"
    private const val KEY_MID = "mid"

    private lateinit var prefs: SharedPreferences

    fun init(app: Application) {
        if (!::prefs.isInitialized) {
            prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    val isLoggedIn: Boolean
        get() = if (!::prefs.isInitialized) false
        else !prefs.getString(KEY_SESSION_ID, null).isNullOrBlank()

    /** Save the cookies read from the WebView after the user logs in. */
    fun saveFromCookieString(cookieString: String?) {
        if (!::prefs.isInitialized || cookieString.isNullOrBlank()) return
        val map = cookieString.split(";").mapNotNull { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2) kv[0].trim() to kv[1].trim() else null
        }.toMap()
        val sessionId = map["sessionid"] ?: return
        prefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putString(KEY_CSRF, map["csrftoken"])
            .putString(KEY_USER_ID, map["ds_user_id"])
            .putString(KEY_MID, map["mid"])
            .apply()
    }

    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().clear().apply()
    }

    /** Cookie header for authenticated Instagram requests. Null when logged out. */
    fun cookieHeader(): String? {
        if (!::prefs.isInitialized) return null
        val sessionId = prefs.getString(KEY_SESSION_ID, null) ?: return null
        val parts = mutableListOf("sessionid=$sessionId")
        prefs.getString(KEY_CSRF, null)?.let { parts += "csrftoken=$it" }
        prefs.getString(KEY_USER_ID, null)?.let { parts += "ds_user_id=$it" }
        prefs.getString(KEY_MID, null)?.let { parts += "mid=$it" }
        return parts.joinToString("; ")
    }

    fun csrfToken(): String? =
        if (!::prefs.isInitialized) null else prefs.getString(KEY_CSRF, null)
}
