package com.linkget.ui.profile

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.Muted

/**
 * Opens Bilibili's official login page and reads the WebView cookie after login.
 * The cookie is stored only in this app's private SharedPreferences and is used for
 * the player API / CDN requests needed by 1080P.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BilibiliLoginDialog(
    currentCookie: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = WebViewClient()
            loadUrl("https://passport.bilibili.com/login")
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color(0xFF0F172A))
                }
                Column(Modifier.weight(1f)) {
                    Text("B 站登录 · 1080P", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "登录后可解析 1080P；Cookie 只保存在本机",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted
                    )
                }
                Button(
                    onClick = {
                        val cookie = CookieManager.getInstance()
                            .getCookie("https://www.bilibili.com").orEmpty()
                        if (cookie.contains("SESSDATA", ignoreCase = true)) {
                            onSave(cookie)
                            Toast.makeText(context, "已保存 B 站登录信息", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "还没检测到登录，请先在页面完成登录", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("完成") }
            }
            if (currentCookie.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        onClear()
                        CookieManager.getInstance().removeAllCookies(null)
                        Toast.makeText(context, "已退出 B 站登录", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }) { Text("退出登录", color = Muted) }
                }
            }
            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp)
            )
            Spacer(Modifier.padding(bottom = 4.dp))
        }
    }
}
