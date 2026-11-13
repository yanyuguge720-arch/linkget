package com.linkget.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val clipboardEnabled: Boolean = false,
    val clipboardDisclosureShown: Boolean = false,
    val saveFolder: String = "LinkGet",
    val preferredQuality: String = "1080P",
    val maxParallel: Int = 2,
    val wifiOnly: Boolean = false,
    val apiBase: String = "",
    val bilibiliCookie: String = "",
    val cloudLiveFallback: Boolean = true
)

/** Small SharedPreferences-backed settings store with a StateFlow surface. */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("linkget_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state

    fun setClipboardEnabled(enabled: Boolean) = edit {
        it.copy(clipboardEnabled = enabled, clipboardDisclosureShown = true)
    }

    fun markClipboardDisclosureShown() = edit { it.copy(clipboardDisclosureShown = true) }

    fun setSaveFolder(folder: String) = edit {
        it.copy(saveFolder = sanitizeFolder(folder))
    }

    fun setPreferredQuality(quality: String) = edit { it.copy(preferredQuality = quality) }

    fun setMaxParallel(value: Int) = edit { it.copy(maxParallel = value.coerceIn(1, 3)) }

    fun setWifiOnly(enabled: Boolean) = edit { it.copy(wifiOnly = enabled) }

    fun setApiBase(url: String) = edit { it.copy(apiBase = url.trim()) }

    fun setBilibiliCookie(cookie: String) = edit { it.copy(bilibiliCookie = cookie.trim()) }

    fun clearBilibiliCookie() = edit { it.copy(bilibiliCookie = "") }

    fun setCloudLiveFallback(enabled: Boolean) = edit { it.copy(cloudLiveFallback = enabled) }

    private fun sanitizeFolder(raw: String): String {
        val cleaned = raw.replace(Regex("""[\\/:*?"<>|\r\n\t]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
        return cleaned.ifBlank { "LinkGet" }.take(32)
    }

    private fun edit(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        prefs.edit()
            .putBoolean(KEY_CLIPBOARD, next.clipboardEnabled)
            .putBoolean(KEY_DISCLOSURE, next.clipboardDisclosureShown)
            .putString(KEY_FOLDER, next.saveFolder)
            .putString(KEY_QUALITY, next.preferredQuality)
            .putInt(KEY_PARALLEL, next.maxParallel)
            .putBoolean(KEY_WIFI, next.wifiOnly)
            .putString(KEY_API, next.apiBase)
            .putString(KEY_BILI_COOKIE, next.bilibiliCookie)
            .putBoolean(KEY_CLOUD_LIVE, next.cloudLiveFallback)
            .apply()
        _state.value = next
    }

    private fun read(): AppSettings = AppSettings(
        clipboardEnabled = prefs.getBoolean(KEY_CLIPBOARD, false),
        clipboardDisclosureShown = prefs.getBoolean(KEY_DISCLOSURE, false),
        saveFolder = prefs.getString(KEY_FOLDER, null)?.takeIf { it.isNotBlank() } ?: "LinkGet",
        preferredQuality = prefs.getString(KEY_QUALITY, null)?.takeIf { it.isNotBlank() } ?: "1080P",
        maxParallel = prefs.getInt(KEY_PARALLEL, 2).coerceIn(1, 3),
        wifiOnly = prefs.getBoolean(KEY_WIFI, false),
        apiBase = prefs.getString(KEY_API, "") ?: "",
        bilibiliCookie = prefs.getString(KEY_BILI_COOKIE, "") ?: "",
        cloudLiveFallback = prefs.getBoolean(KEY_CLOUD_LIVE, true)
    )

    private companion object {
        const val KEY_CLIPBOARD = "clipboard_enabled"
        const val KEY_DISCLOSURE = "clipboard_disclosure_shown"
        const val KEY_FOLDER = "save_folder"
        const val KEY_QUALITY = "preferred_quality"
        const val KEY_PARALLEL = "max_parallel"
        const val KEY_WIFI = "wifi_only"
        const val KEY_API = "api_base"
        const val KEY_BILI_COOKIE = "bilibili_cookie"
        const val KEY_CLOUD_LIVE = "cloud_live_fallback"
    }
}
