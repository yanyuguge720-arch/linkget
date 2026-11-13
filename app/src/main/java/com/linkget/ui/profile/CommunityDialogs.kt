package com.linkget.ui.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.linkget.R
import com.linkget.ui.theme.Muted

@Composable
fun DouyinQrDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("软件动态") },
        text = {
            Column {
                Text(
                    "抖音扫码关注「岩羽朝歌」，获取版本更新和软件动态。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                Image(
                    painter = painterResource(R.drawable.douyin_qr),
                    contentDescription = "抖音二维码",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(410.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "抖音号：zhaogeyanyu",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
                Text(
                    "保存图片后，打开抖音扫一扫，从相册选择即可。",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = saveQrToGallery(context)
                Toast.makeText(
                    context,
                    if (ok) "已保存到相册 Pictures/链取" else "保存失败，请检查存储空间",
                    Toast.LENGTH_SHORT
                ).show()
            }) { Text("保存到相册") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun saveQrToGallery(context: Context): Boolean {
    return runCatching {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.douyin_qr)
            ?: return false
        val name = "链取-抖音二维码-" + System.currentTimeMillis() + ".png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/链取")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)) {
                return false
            }
        } ?: return false
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        bitmap.recycle()
        true
    }.getOrDefault(false)
}

@Composable
fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val group = "35369972"
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("意见反馈") },
        text = {
            Column {
                Text("遇到解析失败、下载异常或想提建议，欢迎加入反馈 QQ 群：", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text("QQ 群", style = MaterialTheme.typography.labelSmall, color = Muted)
                Text(group, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text(
                    "加群请备注「链取反馈」。反馈时请附上：手机型号、系统版本、出问题的链接、截图或录屏。",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString("链取反馈 QQ 群：$group"))
                Toast.makeText(context, "已复制 QQ 群号", Toast.LENGTH_SHORT).show()
            }) { Text("复制群号") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}