package com.linkget.ui.profile

import android.widget.Toast
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkget.BuildConfig
import com.linkget.core.FileNames
import com.linkget.data.library.LocalMediaFile
import com.linkget.data.model.ResourceKind
import com.linkget.ui.components.GridBackground
import com.linkget.ui.components.Pill
import com.linkget.ui.components.openUri
import com.linkget.ui.components.shareUri
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.BlueDeep
import com.linkget.ui.theme.BlueSoft
import com.linkget.ui.theme.Danger
import com.linkget.ui.theme.Ink
import com.linkget.ui.theme.Line
import com.linkget.ui.theme.Muted
import com.linkget.ui.theme.Success
import com.linkget.ui.theme.Warn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editFolder by remember { mutableStateOf(false) }
    var editApi by remember { mutableStateOf(false) }
    var chooseQuality by remember { mutableStateOf(false) }
    var chooseParallel by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showBilibiliLogin by remember { mutableStateOf(false) }
    var legalDoc by remember { mutableStateOf<LegalKind?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var clipboardDisclosure by remember { mutableStateOf(false) }
    var renameFile by remember { mutableStateOf<LocalMediaFile?>(null) }
    var deleteFile by remember { mutableStateOf<LocalMediaFile?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    val visibleFiles = state.files.filter { it.kind == state.tab }
    val totalBytes = state.files.sumOf { it.size }

    GridBackground(modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ProfileHeader(state.files.size, totalBytes) }

            item { GroupTitle("下载与解析") }
            item {
                SettingsGroup {
                    SettingRow(
                        icon = Icons.Rounded.FolderOpen,
                        title = "保存文件夹",
                        subtitle = "Movies/" + settings.saveFolder,
                        onClick = { editFolder = true }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.HighQuality,
                        title = "默认视频清晰度",
                        subtitle = "优先选择更高的清晰度",
                        value = settings.preferredQuality,
                        onClick = { chooseQuality = true }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.Download,
                        title = "同时下载数量",
                        subtitle = "多个任务并行，默认 2 个",
                        value = settings.maxParallel.toString() + " 个",
                        onClick = { chooseParallel = true }
                    )
                    Divider()
                    SettingSwitchRow(
                        icon = Icons.Rounded.Wifi,
                        title = "仅 Wi-Fi 下载",
                        subtitle = "移动网络下暂停任务，连上 Wi-Fi 后继续",
                        checked = settings.wifiOnly,
                        onCheckedChange = viewModel::setWifiOnly
                    )
                    Divider()
                    SettingSwitchRow(
                        icon = Icons.Rounded.ContentPaste,
                        title = "剪贴板自动识别",
                        subtitle = "回到 App 时检测链接并询问是否填入",
                        checked = settings.clipboardEnabled,
                        onCheckedChange = { checked ->
                            if (checked) clipboardDisclosure = true
                            else viewModel.setClipboardEnabled(false)
                        }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.Cloud,
                        title = "解析服务",
                        subtitle = if (settings.apiBase.isBlank()) "未配置 · 使用本机解析" else settings.apiBase,
                        value = if (settings.apiBase.isBlank()) "本地" else "已配置",
                        onClick = { editApi = true }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.AccountCircle,
                        title = "B 站 1080P 登录",
                        subtitle = if (settings.bilibiliCookie.isBlank()) {
                            "未登录时最高 720P；登录后可解析 1080P"
                        } else {
                            "已登录，解析时会带上登录 Cookie"
                        },
                        value = if (settings.bilibiliCookie.isBlank()) "去登录" else "已登录",
                        onClick = { showBilibiliLogin = true }
                    )
                    Divider()
                    SettingSwitchRow(
                        icon = Icons.Rounded.Cloud,
                        title = "实况图云端补全",
                        subtitle = "本地接口拿不到实况时使用云端解析；链接会发送到第三方",
                        checked = settings.cloudLiveFallback,
                        onCheckedChange = viewModel::setCloudLiveFallback
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupTitle("本地文件")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = viewModel::refresh) {
                        Text("刷新", style = MaterialTheme.typography.labelMedium, color = Blue)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryTab.entries.forEach { tab ->
                        FilterChip(
                            selected = state.tab == tab.kind,
                            onClick = { viewModel.setTab(tab.kind) },
                            label = { Text(tab.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.78f)
                            )
                        )
                    }
                }
            }

            if (state.loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                    }
                }
            } else if (visibleFiles.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 30.dp, horizontal = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (state.tab) {
                                    ResourceKind.VIDEO -> Icons.Rounded.VideoFile
                                    ResourceKind.AUDIO -> Icons.Rounded.MusicNote
                                    ResourceKind.IMAGE -> Icons.Rounded.Image
                                },
                                contentDescription = null,
                                tint = Blue.copy(alpha = 0.6f),
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("这里还没有" + when (state.tab) { ResourceKind.VIDEO -> "视频"; ResourceKind.AUDIO -> "音频"; ResourceKind.IMAGE -> "图片" } + "文件", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "下载完成后会自动出现在这里",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                    }
                }
            } else {
                items(visibleFiles, key = { it.uri.toString() }) { file ->
                    LocalFileRow(
                        file = file,
                        onOpen = { openUri(context, file.uri, file.mime) },
                        onShare = { shareUri(context, file.uri, file.mime, "分享文件") },
                        onRename = { renameFile = file },
                        onDelete = { deleteFile = file }
                    )
                }
            }

            item { GroupTitle("支持") }
            item {
                SettingsGroup {
                    SettingRow(
                        icon = Icons.Rounded.Feedback,
                        title = "意见反馈",
                        subtitle = "QQ 群 35369972",
                        onClick = { showFeedback = true }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.QrCode,
                        title = "软件动态",
                        subtitle = "抖音扫码关注 · 岩羽朝歌",
                        onClick = { showQr = true }
                    )
                }
            }

            item { GroupTitle("关于") }
            item {
                SettingsGroup {
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        title = "关于链取",
                        subtitle = "版本 " + BuildConfig.VERSION_NAME + " · 五个平台",
                        onClick = { showAbout = true }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.Shield,
                        title = "隐私政策",
                        subtitle = "本地优先 · 链接与下载记录不上传",
                        onClick = { legalDoc = LegalKind.PRIVACY }
                    )
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        title = "免责声明",
                        subtitle = "仅供个人备份，禁止商用与二次分发",
                        onClick = { legalDoc = LegalKind.DISCLAIMER }
                    )
                }
            }

            item {
                Text(
                    text = "链取 · 本地优先的多平台媒体解析工具\n下载内容版权归原平台与著作权人所有",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted.copy(alpha = 0.85f),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (editFolder) {
        TextInputDialog(
            title = "保存文件夹",
            message = "文件会保存到 Movies / Pictures / Music 下的同名子文件夹。",
            initial = settings.saveFolder,
            placeholder = "链取",
            onDismiss = { editFolder = false },
            onConfirm = {
                viewModel.setSaveFolder(it)
                viewModel.refresh()
                editFolder = false
            }
        )
    }

    if (editApi) {
        TextInputDialog(
            title = "解析服务地址",
            message = "可选。配置后会优先调用后端 /api/parse；留空则使用手机本地解析。",
            initial = settings.apiBase,
            placeholder = "https://example.com",
            onDismiss = { editApi = false },
            onConfirm = {
                viewModel.setApiBase(it)
                editApi = false
            }
        )
    }

    if (chooseQuality) {
        ChoiceDialog(
            title = "默认视频清晰度",
            options = listOf("1080P", "720P"),
            selected = settings.preferredQuality,
            onDismiss = { chooseQuality = false },
            onSelect = {
                viewModel.setPreferredQuality(it)
                chooseQuality = false
            }
        )
    }

    if (chooseParallel) {
        ChoiceDialog(
            title = "同时下载数量",
            options = listOf("1", "2", "3"),
            selected = settings.maxParallel.toString(),
            onDismiss = { chooseParallel = false },
            onSelect = {
                viewModel.setMaxParallel(it.toIntOrNull() ?: 2)
                chooseParallel = false
            }
        )
    }

    if (clipboardDisclosure) {
        AlertDialog(
            onDismissRequest = { clipboardDisclosure = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("开启剪贴板识别") },
            text = {
                Text(
                    "开启后，链取只会在你打开或切回 App 时读取一次剪贴板，用于识别平台链接并询问是否填入。\n\n" +
                        "内容只在本机处理，不会上传或保存；Android 12 及以上会显示系统「已粘贴」提示。你可以随时关闭。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardDisclosure = false
                    viewModel.setClipboardEnabled(true)
                }) { Text("开启") }
            },
            dismissButton = {
                TextButton(onClick = { clipboardDisclosure = false }) { Text("取消") }
            }
        )
    }

    renameFile?.let { file ->
        TextInputDialog(
            title = "重命名文件",
            message = "扩展名会保留。",
            initial = file.name.substringBeforeLast('.', file.name),
            placeholder = "文件名",
            onDismiss = { renameFile = null },
            onConfirm = { name ->
                viewModel.rename(file, name)
                renameFile = null
            }
        )
    }

    deleteFile?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteFile = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("删除这个文件？") },
            text = { Text(file.name + "\n\n删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(file)
                    deleteFile = null
                }) { Text("删除", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { deleteFile = null }) { Text("取消") } }
        )
    }

    if (showBilibiliLogin) {
        BilibiliLoginDialog(
            currentCookie = settings.bilibiliCookie,
            onSave = viewModel::setBilibiliCookie,
            onClear = viewModel::clearBilibiliCookie,
            onDismiss = { showBilibiliLogin = false }
        )
    }

    legalDoc?.let { kind ->
        LegalDialog(kind = kind, onDismiss = { legalDoc = null })
    }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }

    if (showQr) {
        DouyinQrDialog(onDismiss = { showQr = false })
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

private enum class LibraryTab(val label: String, val kind: ResourceKind) {
    VIDEO("视频", ResourceKind.VIDEO),
    AUDIO("音频", ResourceKind.AUDIO),
    IMAGE("图片", ResourceKind.IMAGE)
}

@Composable
private fun ProfileHeader(fileCount: Int, totalBytes: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(BlueDeep, Blue)
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "链取",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "本地优先 · 无需账号",
                            color = Color(0xFFDBEAFE),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Pill("v" + BuildConfig.VERSION_NAME, color = Color.White, background = Color.White.copy(alpha = 0.16f))
                }
                Spacer(Modifier.height(18.dp))
                Row {
                    Stat("本地文件", fileCount.toString() + " 个", Modifier.weight(1f))
                    Stat("占用空间", FileNames.formatBytes(totalBytes).ifBlank { "0 B" }, Modifier.weight(1f))
                    Stat("核心能力", "解析 · 下载", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFFBFDBFE), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Ink,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.labelMedium, color = Blue)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 2)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Blue
            )
        )
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(BlueSoft),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 59.dp),
        color = Line.copy(alpha = 0.7f)
    )
}

@Composable
private fun LocalFileRow(
    file: LocalMediaFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        when (file.kind) {
                            ResourceKind.VIDEO -> BlueSoft
                            ResourceKind.AUDIO -> Color(0xFFF3E8FF)
                            ResourceKind.IMAGE -> Color(0xFFE8F8EF)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (file.kind) {
                        ResourceKind.VIDEO -> Icons.Rounded.VideoFile
                        ResourceKind.AUDIO -> Icons.Rounded.MusicNote
                        ResourceKind.IMAGE -> Icons.Rounded.Image
                    },
                    contentDescription = null,
                    tint = when (file.kind) {
                        ResourceKind.VIDEO -> Blue
                        ResourceKind.AUDIO -> Color(0xFF7E22CE)
                        ResourceKind.IMAGE -> Success
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = FileNames.formatBytes(file.size) + " · " +
                        SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(file.dateAdded)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(31.dp)) {
                Icon(Icons.Rounded.Share, contentDescription = "分享", tint = Muted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onRename, modifier = Modifier.size(31.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "重命名", tint = Muted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(31.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = Muted, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    message: String,
    initial: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(placeholder) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = Line
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(option) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (option == selected) BlueSoft else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (option == selected) Blue.copy(alpha = 0.25f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(option, style = MaterialTheme.typography.bodyMedium, color = Ink)
                            Spacer(Modifier.weight(1f))
                            if (option == selected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Blue)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text("关于链取") },
        text = {
            Column {
                Text("版本 " + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(10.dp))
                Text(
                    "一个本地优先的链接解析工具：粘贴抖音 / B 站 / 小红书 / 快手 / 今日头条分享链接，提取视频、音频与图集，预览后保存到手机。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text("支持站点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("抖音：视频 / 图集 / 背景音乐", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("哔哩哔哩：720P / 1080P / 音频", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("小红书：图文 / 视频笔记", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("快手：短视频", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("今日头条：视频 / 图文", style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(8.dp))
                Text("反馈：QQ 群 35369972", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("B 站 1080P：需在「我的」登录 B 站", style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF7E8)) {
                    Text(
                        text = "免责声明：本工具仅供个人自用备份。下载的媒体资源版权归原平台与著作权人所有，禁止商用、二次分发或传播侵权内容。",
                        modifier = Modifier.padding(11.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Warn,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}