package com.linkget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.linkget.core.ClipboardReader
import com.linkget.data.settings.SettingsRepository
import com.linkget.ui.LinkGetRoot
import com.linkget.ui.theme.LinkGetTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as LinkGetApp).container }
    private var checkClipboardOnFocus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        setContent {
            LinkGetTheme {
                LinkGetRoot(container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.downloads.resumeIfNeeded()
        val settings = container.settings.state.value
        if (settings.clipboardEnabled) {
            // Android 10+ only allows clipboard reads once the window has focus. onResume
            // can arrive just before that, so also retry in onWindowFocusChanged.
            checkClipboardOnFocus = true
            container.clipboardReads.tryEmit(ClipboardReader.read(this))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && checkClipboardOnFocus) {
            checkClipboardOnFocus = false
            container.clipboardReads.tryEmit(ClipboardReader.read(this))
        }
    }
}
