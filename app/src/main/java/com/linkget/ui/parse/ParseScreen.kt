package com.linkget.ui.parse

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkget.data.db.HistoryEntry
import com.linkget.ui.components.GlassCard
import com.linkget.ui.components.GridBackground
import com.linkget.ui.components.PlatformBadge
import com.linkget.ui.components.Pill
import com.linkget.ui.components.formatRelativeTime
import com.linkget.ui.result.ResultScreen
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.BlueSoft
import com.linkget.ui.theme.Danger
import com.linkget.ui.theme.DangerSoft
import com.linkget.ui.theme.Ink
import com.linkget.ui.theme.Muted
import com.linkget.ui.theme.Success

@Composable
fun ParseScreen(
    viewModel: ParseViewModel,
    onQueued: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recent by viewModel.recentHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.queuedCount) {
        if (state.queuedCount > 0) {
            onQueued(state.queuedCount)
            viewModel.consumeQueued()
        }
    }

    state.result?.let { work ->
        ResultScreen(
            work = work,
            selected = state.selected,
            saving = state.saving,
            onBack = viewModel::dismissResult,
            onToggle = viewModel::toggleResource,
            onSelectAll = viewModel::selectAll,
            onClearSelection = viewModel::clearSelection,
            onToggleExtractAudio = viewModel::toggleExtractAudio,
            extractAudio = state.extractAudio,
            onDownload = { viewModel.enqueueSelected() },
            modifier = modifier
        )
        return
    }

    GridBackground(modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { BrandHeader() }
            item { Hero() }
            item {
                ParseCard(
                    input = state.input,
                    parsing = state.parsing,
                    error = state.error,
                    onInputChange = viewModel::onInputChange,
                    onPaste = { viewModel.paste(com.linkget.core.ClipboardReader.read(context)) },
                    onClear = viewModel::clearInput,
                    onParse = viewModel::parse,
                    onCancel = viewModel::cancelParse
                )
            }
            if (recent.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最近解析",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "在历史中查看全部",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(recent.take(2), key = { it.id }) { entry ->
                    RecentHistoryRow(entry) { viewModel.parseUrl(entry.sourceUrl) }
                }
            }
            item { HowItWorksCard() }
            item { Disclaimer() }
        }
    }

    state.clipboardSuggestion?.let { suggestion ->
        AlertDialog(
            onDismissRequest = viewModel::dismissClipboardSuggestion,
            shape = RoundedCornerShape(24.dp),
            title = { Text("检测到剪贴板链接") },
            text = {
                Column {
                    Text("要填入这条链接并解析吗？")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = suggestion.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = Blue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::acceptClipboardSuggestion) { Text("填入") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClipboardSuggestion) { Text("忽略") }
            }
        )
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Blue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "链取",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "多平台音视频解析",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Hero() {
    Column(Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Text(
            text = "把喜欢的内容，\n留在手机里。",
            style = MaterialTheme.typography.displaySmall,
            fontSize = 31.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "粘贴分享链接，解析视频、音频与图集；勾选后保存到本地。",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Pill("抖音", color = Color(0xFF111827), background = Color(0xFFF1F5F9))
            Pill("哔哩哔哩", color = Color(0xFFBE185D), background = Color(0xFFFDF2F8))
            Pill("小红书", color = Color(0xFFDC2626), background = Color(0xFFFEF2F2))
            Pill("快手", color = Color(0xFFEA580C), background = Color(0xFFFFF7ED))
            Pill("头条", color = Color(0xFFE11D48), background = Color(0xFFFFF1F2))
        }
    }
}

@Composable
private fun ParseCard(
    input: String,
    parsing: Boolean,
    error: String?,
    onInputChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onParse: () -> Unit,
    onCancel: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    GlassCard(modifier = Modifier.fillMaxWidth(), corner = 26.dp, contentPadding = 18.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Link,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "分享链接",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "支持抖音 / B 站 / 小红书 / 快手 / 今日头条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Success)
                )
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "粘贴分享链接或整段文案\n例如：https://v.douyin.com/…",
                        color = Muted.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color(0xFFF8FAFF),
                    unfocusedContainerColor = Color(0xFFFAFBFD)
                ),
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "清空",
                                modifier = Modifier.size(18.dp),
                                tint = Muted
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    onParse()
                })
            )

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPaste, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("粘贴", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("清空", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (parsing) "解析中，最长 25 秒" else "一次解析全部资源",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (parsing) Blue else Muted
                )
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DangerSoft,
                    border = BorderStroke(1.dp, Danger.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Danger,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (parsing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEFF4FF)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        color = Blue,
                        strokeWidth = 2.2.dp
                    )
                    Spacer(Modifier.width(9.dp))
                    Text("正在解析资源…", color = Blue, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("取消解析", color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Button(
                    onClick = {
                        keyboard?.hide()
                        onParse()
                    },
                    enabled = input.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue,
                        contentColor = Color.White,
                        disabledContainerColor = Blue.copy(alpha = 0.28f),
                        disabledContentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("开始解析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentHistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatRelativeTime(entry.parsedAt) + " · " + entry.sourceUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "重新解析",
                style = MaterialTheme.typography.labelSmall,
                color = Blue
            )
        }
    }
}

@Composable
private fun HowItWorksCard() {
    GlassCard(modifier = Modifier.fillMaxWidth(), corner = 24.dp, contentPadding = 18.dp) {
        Column {
            Text(
                text = "三步拿到文件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            StepRow("1", "在原平台复制分享链接", Icons.Rounded.ContentPaste)
            StepRow("2", "回到链取粘贴并解析", Icons.Rounded.Search)
            StepRow("3", "预览、勾选，加入下载队列", Icons.Rounded.Download)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Movie, null, modifier = Modifier.size(15.dp), tint = Blue)
                Spacer(Modifier.width(6.dp))
                Text("视频", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(15.dp), tint = Blue)
                Spacer(Modifier.width(6.dp))
                Text("音频", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Rounded.Link, null, modifier = Modifier.size(15.dp), tint = Blue)
                Spacer(Modifier.width(6.dp))
                Text("图集", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
    }
}

@Composable
private fun StepRow(number: String, text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(BlueSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = Blue,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(9.dp))
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Muted)
        Spacer(Modifier.width(7.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}

@Composable
private fun Disclaimer() {
    Text(
        text = "仅供个人备份使用。下载内容的版权归原平台与著作权人所有，请勿商用、二次分发或传播侵权内容。",
        style = MaterialTheme.typography.labelSmall,
        color = Muted.copy(alpha = 0.85f),
        lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}