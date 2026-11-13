package com.linkget.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linkget.core.FileNames
import com.linkget.data.db.DownloadStatus
import com.linkget.data.db.DownloadTaskEntity
import com.linkget.data.db.toDownloadStatus
import com.linkget.data.model.Platform
import com.linkget.data.model.ResourceKind
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.BlueSoft
import com.linkget.ui.theme.Danger
import com.linkget.ui.theme.DangerSoft
import com.linkget.ui.theme.Faint
import com.linkget.ui.theme.Muted
import com.linkget.ui.theme.Success
import com.linkget.ui.theme.SuccessSoft
import com.linkget.ui.theme.Warn
import com.linkget.ui.theme.WarnSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun Pill(
    text: String,
    color: Color = Blue,
    background: Color = BlueSoft,
    modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(50), color = background, modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PlatformBadge(platform: Platform, showName: Boolean = true) {
    val (bg, fg) = when (platform) {
        Platform.DOUYIN -> Color(0xFF111111) to Color.White
        Platform.BILIBILI -> Color(0xFFFB7299) to Color.White
        Platform.XIAOHONGSHU -> Color(0xFFFF2442) to Color.White
        Platform.KUAISHOU -> Color(0xFFFF7A00) to Color.White
        Platform.TOUTIAO -> Color(0xFFE11D48) to Color.White
        Platform.GENERIC -> Blue to Color.White
        Platform.UNKNOWN -> Faint to Color.White
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = platform.badge,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
        if (showName) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = platform.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun kindLabel(kind: ResourceKind): String = when (kind) {
    ResourceKind.VIDEO -> "视频"
    ResourceKind.AUDIO -> "音频"
    ResourceKind.IMAGE -> "图片"
}

fun kindIcon(kind: ResourceKind): ImageVector = when (kind) {
    ResourceKind.VIDEO -> Icons.Rounded.Movie
    ResourceKind.AUDIO -> Icons.Rounded.MusicNote
    ResourceKind.IMAGE -> Icons.Rounded.Image
}

@Composable
fun KindBadge(kind: ResourceKind, modifier: Modifier = Modifier) {
    val bg = when (kind) {
        ResourceKind.VIDEO -> BlueSoft
        ResourceKind.AUDIO -> Color(0xFFF3E8FF)
        ResourceKind.IMAGE -> Color(0xFFE8F8EF)
    }
    val fg = when (kind) {
        ResourceKind.VIDEO -> Blue
        ResourceKind.AUDIO -> Color(0xFF7E22CE)
        ResourceKind.IMAGE -> Success
    }
    Surface(modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = bg) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(kindIcon(kind), contentDescription = null, modifier = Modifier.size(20.dp), tint = fg)
        }
    }
}

@Composable
fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.PENDING -> "等待下载"
    DownloadStatus.RUNNING -> "下载中"
    DownloadStatus.PAUSED -> "已暂停"
    DownloadStatus.COMPLETED -> "下载成功"
    DownloadStatus.FAILED -> "下载失败"
}

@Composable
fun statusColor(status: DownloadStatus): Color = when (status) {
    DownloadStatus.RUNNING -> Blue
    DownloadStatus.PENDING -> Muted
    DownloadStatus.PAUSED -> Warn
    DownloadStatus.COMPLETED -> Success
    DownloadStatus.FAILED -> Danger
}

fun statusBackground(status: DownloadStatus): Color = when (status) {
    DownloadStatus.RUNNING -> BlueSoft
    DownloadStatus.PENDING -> Color(0xFFF1F5F9)
    DownloadStatus.PAUSED -> WarnSoft
    DownloadStatus.COMPLETED -> SuccessSoft
    DownloadStatus.FAILED -> DangerSoft
}

fun formatRelativeTime(timeMillis: Long): String {
    val diff = System.currentTimeMillis() - timeMillis
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> (diff / 60_000).toString() + " 分钟前"
        diff < 86_400_000 -> (diff / 3_600_000).toString() + " 小时前"
        diff < 7 * 86_400_000L -> (diff / 86_400_000).toString() + " 天前"
        else -> SimpleDateFormat("MM-dd", Locale.CHINA).format(Date(timeMillis))
    }
}

fun DownloadTaskEntity.statusEnum(): DownloadStatus = status.toDownloadStatus()

fun sizeText(task: DownloadTaskEntity): String = when {
    task.totalBytes > 0 && task.statusEnum() == DownloadStatus.RUNNING ->
        FileNames.formatBytes(task.bytesDownloaded) + " / " + FileNames.formatBytes(task.totalBytes)
    task.totalBytes > 0 -> FileNames.formatBytes(task.totalBytes)
    task.bytesDownloaded > 0 -> FileNames.formatBytes(task.bytesDownloaded)
    else -> ""
}

fun openSavedFile(context: Context, task: DownloadTaskEntity) {
    val uri = task.localUri?.let(Uri::parse) ?: return
    openUri(context, uri, FileNames.mimeType(task.fileName))
}

fun openUri(context: Context, uri: Uri, mime: String) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "没有可以打开此文件的应用", Toast.LENGTH_SHORT).show() }
}

fun shareUri(context: Context, uri: Uri, mime: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType(mime)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(Intent.createChooser(intent, title)) }
        .onFailure { Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show() }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(modifier = Modifier.size(78.dp), shape = RoundedCornerShape(26.dp), color = BlueSoft) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = Blue)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}
