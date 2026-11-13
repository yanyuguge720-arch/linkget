package com.linkget.ui.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkget.core.FileNames
import com.linkget.data.db.DownloadStatus
import com.linkget.data.db.DownloadTaskEntity
import com.linkget.data.model.ResourceKind
import com.linkget.data.model.formatDuration
import com.linkget.ui.components.EmptyState
import com.linkget.ui.components.GridBackground
import com.linkget.ui.components.KindBadge
import com.linkget.ui.components.Pill
import com.linkget.ui.components.openSavedFile
import com.linkget.ui.components.sizeText
import com.linkget.ui.components.statusBackground
import com.linkget.ui.components.statusColor
import com.linkget.ui.components.statusEnum
import com.linkget.ui.components.statusLabel
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.BlueDeep
import com.linkget.ui.theme.BlueSoft
import com.linkget.ui.theme.Danger
import com.linkget.ui.theme.Ink
import com.linkget.ui.theme.Muted
import com.linkget.ui.theme.Success
import com.linkget.ui.theme.Warn

private enum class TaskFilter(val label: String) {
    ALL("全部"), ACTIVE("进行中"), DONE("已完成"), FAILED("失败")
}

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel, modifier: Modifier = Modifier) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var confirmClear by remember { mutableStateOf(false) }

    val visible = when (filter) {
        TaskFilter.ALL -> tasks
        TaskFilter.ACTIVE -> tasks.filter {
            it.statusEnum() == DownloadStatus.RUNNING ||
                it.statusEnum() == DownloadStatus.PENDING ||
                it.statusEnum() == DownloadStatus.PAUSED
        }
        TaskFilter.DONE -> tasks.filter { it.statusEnum() == DownloadStatus.COMPLETED }
        TaskFilter.FAILED -> tasks.filter { it.statusEnum() == DownloadStatus.FAILED }
    }
    val active = tasks.filter {
        it.statusEnum() == DownloadStatus.RUNNING || it.statusEnum() == DownloadStatus.PENDING
    }
    val paused = tasks.filter { it.statusEnum() == DownloadStatus.PAUSED }
    val failed = tasks.filter { it.statusEnum() == DownloadStatus.FAILED }
    val done = tasks.filter { it.statusEnum() == DownloadStatus.COMPLETED }
    val activeBytes = active.sumOf { it.bytesDownloaded }
    val totalBytes = active.sumOf { it.totalBytes }
    val speed = active.sumOf { it.speedBytesPerSecond }

    GridBackground(modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "下载任务",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text(
                            text = "支持暂停、继续与多任务并行",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                    if (done.isNotEmpty() || failed.isNotEmpty() || paused.isNotEmpty()) {
                        IconButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "清理记录", tint = Muted)
                        }
                    }
                }
            }

            if (tasks.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskFilter.entries.forEach { item ->
                            FilterChip(
                                selected = filter == item,
                                onClick = { filter = item },
                                label = { Text(item.label) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.75f)
                                )
                            )
                        }
                    }
                }
            }

            if (active.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = BlueDeep,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "正在下载 " + active.size + " 个任务",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = if (speed > 0) FileNames.formatBytes(speed) + "/s" else "准备中",
                                    color = Color(0xFFBFDBFE),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            val fraction = if (totalBytes > 0) (activeBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = Color(0xFF93C5FD),
                                trackColor = Color.White.copy(alpha = 0.18f)
                            )
                            Spacer(Modifier.height(7.dp))
                            Row {
                                Text(
                                    text = if (totalBytes > 0) (fraction * 100).toInt().toString() + "%" else "等待中",
                                    color = Color(0xFFDBEAFE),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = FileNames.formatBytes(activeBytes) + " / " + FileNames.formatBytes(totalBytes),
                                    color = Color(0xFFDBEAFE),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            if (paused.isNotEmpty() || failed.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (paused.isNotEmpty()) {
                            OutlinedAction("继续全部", Icons.Rounded.PlayArrow) { viewModel.resumeAll() }
                        }
                        if (failed.isNotEmpty()) {
                            OutlinedAction("重试失败", Icons.Rounded.Refresh) { viewModel.retryAll() }
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(440.dp)) {
                        EmptyState(
                            icon = if (tasks.isEmpty()) Icons.Rounded.FolderOpen else Icons.Rounded.Download,
                            title = if (tasks.isEmpty()) "还没有下载任务" else "这个分类暂时是空的",
                            body = if (tasks.isEmpty()) "去解析页粘贴一条链接，勾选资源后会出现在这里。" else "切换上方筛选看看其他任务。"
                        )
                    }
                }
            } else {
                items(visible, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onOpen = { openSavedFile(context, task) },
                        onPause = { viewModel.pause(task.id) },
                        onResume = { viewModel.resume(task.id) },
                        onDelete = { viewModel.delete(task.id) }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("清理下载记录？") },
            text = { Text("只会移除已完成、失败和已暂停的任务记录，已下载到手机的文件不会删除。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearFinished()
                }) { Text("清理", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun OutlinedAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Color.White),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(15.dp), tint = Blue)
            Spacer(Modifier.width(5.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = Blue)
        }
    }
}

@Composable
private fun TaskCard(
    task: DownloadTaskEntity,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val status = task.statusEnum()
    val kind = runCatching { ResourceKind.valueOf(task.kind) }.getOrDefault(ResourceKind.VIDEO)
    val fraction = if (task.totalBytes > 0) {
        (task.bytesDownloaded.toFloat() / task.totalBytes).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = status == DownloadStatus.COMPLETED, onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindBadge(kind)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusLabel(status),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor(status),
                            fontWeight = FontWeight.Medium
                        )
                        val size = sizeText(task)
                        if (size.isNotEmpty()) {
                            Text(
                                text = "  ·  " + size,
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted
                            )
                        }
                    }
                }
                Pill(
                    text = task.quality ?: kind.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = statusColor(status),
                    background = statusBackground(status)
                )
            }

            if (status == DownloadStatus.RUNNING) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = Blue,
                    trackColor = Color(0xFFE8EEF7)
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (fraction * 100).toInt().toString() + "%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Blue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (task.speedBytesPerSecond > 0) FileNames.formatBytes(task.speedBytesPerSecond) + "/s" else "连接中…",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = sizeText(task),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted
                    )
                }
            }

            if (status == DownloadStatus.PENDING) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = Blue.copy(alpha = 0.45f),
                    trackColor = Color(0xFFE8EEF7)
                )
            }

            if (status == DownloadStatus.PAUSED && !task.error.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Pause, null, modifier = Modifier.size(13.dp), tint = Warn)
                    Spacer(Modifier.width(5.dp))
                    Text(task.error ?: "", style = MaterialTheme.typography.labelSmall, color = Warn)
                }
            }

            if (status == DownloadStatus.FAILED && !task.error.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.ErrorOutline, null, modifier = Modifier.size(14.dp), tint = Danger)
                    Spacer(Modifier.width(5.dp))
                    Text(task.error ?: "", style = MaterialTheme.typography.labelSmall, color = Danger)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val duration = formatDuration(task.durationMs)
                if (duration.isNotEmpty()) {
                    Text(duration, style = MaterialTheme.typography.labelSmall, color = Muted)
                }
                Spacer(Modifier.weight(1f))
                when (status) {
                    DownloadStatus.RUNNING, DownloadStatus.PENDING -> {
                        TextButton(onClick = onPause, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Rounded.Pause, null, modifier = Modifier.size(16.dp), tint = Blue)
                            Spacer(Modifier.width(3.dp))
                            Text("暂停", style = MaterialTheme.typography.labelSmall, color = Blue)
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        TextButton(onClick = onResume, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(17.dp), tint = Blue)
                            Text("继续", style = MaterialTheme.typography.labelSmall, color = Blue)
                        }
                    }
                    DownloadStatus.FAILED -> {
                        TextButton(onClick = onResume, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp), tint = Blue)
                            Text("重试", style = MaterialTheme.typography.labelSmall, color = Blue)
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Blue)
                            Text("打开", style = MaterialTheme.typography.labelSmall, color = Blue)
                        }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = Muted, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}
