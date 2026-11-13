package com.linkget.ui.history

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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkget.data.db.HistoryEntry
import com.linkget.data.model.Platform
import com.linkget.ui.components.EmptyState
import com.linkget.ui.components.GridBackground
import com.linkget.ui.components.NetworkImage
import com.linkget.ui.components.PlatformBadge
import com.linkget.ui.components.formatRelativeTime
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.Danger
import com.linkget.ui.theme.Ink
import com.linkget.ui.theme.Muted

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onReparse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

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
                            text = "解析历史",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text(
                            text = "点击记录可重新解析，链接只保存在本机",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                    if (entries.isNotEmpty()) {
                        IconButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "清空历史", tint = Muted)
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(460.dp)) {
                        EmptyState(
                            icon = Icons.Rounded.History,
                            title = "还没有解析记录",
                            body = "解析成功后会在这里留下链接与作品信息，卸载 App 后记录会一并清除。"
                        )
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    HistoryCard(
                        entry = entry,
                        onOpen = { onReparse(entry.sourceUrl) },
                        onDelete = { viewModel.delete(entry.id) }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("清空全部历史？") },
            text = { Text("只删除本机的解析记录，不会删除已下载的文件。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearAll()
                }) { Text("清空", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, onOpen: () -> Unit, onDelete: () -> Unit) {
    val platform = runCatching { Platform.valueOf(entry.platform) }.getOrDefault(Platform.UNKNOWN)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NetworkImage(
                url = entry.coverUrl,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(15.dp)),
                cornerRadius = 15,
                maxDimension = 320
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(platform)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(entry.parsedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = entry.sourceUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onOpen, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = "重新解析", tint = Blue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除记录", tint = Muted, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}
