package com.linkget.core

import android.content.ClipboardManager
import android.content.Context
import android.os.Build

/**
 * Reads the clipboard once, only when the app is in the foreground. There is deliberately no
 * background listener: that would be a privacy problem and would drain the battery.
 */
object ClipboardReader {

    fun read(context: Context): String? {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        return runCatching {
            if (!manager.hasPrimaryClip()) return null
            val clip = manager.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    /** Android 12+ shows a system toast whenever a non-IME app reads the clipboard. */
    fun systemToastNote(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "系统会提示「已粘贴」" else null
}
