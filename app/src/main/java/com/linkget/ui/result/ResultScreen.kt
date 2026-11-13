package com.linkget.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.model.ResourceKind
import com.linkget.data.model.formatDuration
import com.linkget.data.model.formatSize
import com.linkget.ui.components.GlassBorderSurface
import com.linkget.ui.components.GlassCard
import com.linkget.ui.components.GridBackground
import com.linkget.ui.components.NetworkImage
import com.linkget.ui.components.PlatformBadge
import com.linkget.ui.components.Pill
import com.linkget.ui.components.SectionHeader
import com.linkget.ui.components.kindIcon
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.BlueDeep
import com.linkget.ui.theme.BlueSoft
import com.linkget.ui.theme.Ink
import com.linkget.ui.theme.Muted
import com.linkget.ui.theme.Success

@Composable
fun ResultScreen(
    work: ParsedWork,
    selected: Set<String>,
    saving: Boolean,
    onBack: () -> Unit,
    onToggle: (MediaResource) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleExtractAudio: () -> Unit,
    extractAudio: Boolean,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previewVideo by remember { mutableStateOf<MediaResource?>(null) }
    var previewImages by remember { mutableStateOf<List<MediaResource>>(emptyList()) }
    var previewImageIndex by remember { mutableIntStateOf(0) }

    val chosen = work.resources.filter { it.key in selected }
    val chosenBytes = chosen.sumOf { it.sizeBytes ?: 0L }

    GridBackground(modifier) {
        Column(Modifier.fillMaxSize()) {
            ResultTopBar(work = work, onBack = onBack)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { WorkHeader(work) }
                if (work.normalVideos.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "视频资源",
                            subtitle = "不同清晰度会分别下载"
                        ) {
                            Pill(text = work.normalVideos.size.toString() + " 个", color = Blue, background = BlueSoft)
                        }
                    }
                    items(work.normalVideos.size, key = { work.normalVideos[it].key }) { index ->
                        val resource = work.normalVideos[index]
                        ResourceRow(
                            resource = resource,
                            coverUrl = work.coverUrl,
                            selected = resource.key in selected,
                            onToggle = { onToggle(resource) },
                            onPreview = { previewVideo = resource }
                        )
                    }
                    if (work.audios.isEmpty() && work.normalVideos.any { it.key in selected }) {
                        item {
                            ExtractAudioRow(checked = extractAudio, onToggle = onToggleExtractAudio)
                        }
                    }
                }
                if (work.audios.isNotEmpty()) {
                    item { SectionHeader(title = "音频资源", subtitle = "可单独试听与下载") }
                    items(work.audios.size, key = { work.audios[it].key }) { index ->
                        val resource = work.audios[index]
                        ResourceRow(
                            resource = resource,
                            coverUrl = null,
                            selected = resource.key in selected,
                            onToggle = { onToggle(resource) },
                            onPreview = { previewVideo = resource }
                        )
                    }
                }
                if (work.images.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = if (work.isLivePhoto) "实况照片 / 图集" else "图片 / 图集",
                            subtitle = if (work.isLivePhoto) {
                                "勾选实况图会合成为 _MP.jpg 动态照片"
                            } else {
                                "点击图片预览，右上角勾选"
                            }
                        ) {
                            Pill(text = work.images.size.toString() + " 张", color = Success, background = Color(0xFFE8F8EF))
                        }
                    }
                    if (work.isLivePhoto) {
                        item {
                            Text(
                                text = "实况照片下载后会合成安卓 Motion Photo（文件名以 _MP.jpg 结尾），相册里可长按或点开播放动态；原始 MP4 短片可在下方单独下载。",
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                    item {
                        ImageGrid(
                            images = work.images,
                            selected = selected,
                            onToggle = onToggle,
                            onPreview = { index ->
                                previewImages = work.images
                                previewImageIndex = index
                            }
                        )
                    }
                }
                if (work.liveMotions.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "实况动态",
                            subtitle = "抖音实况的短 MP4，可单独预览和下载"
                        ) {
                            Pill(text = work.liveMotions.size.toString() + " 个", color = Color(0xFF7E22CE), background = Color(0xFFF3E8FF))
                        }
                    }
                    items(work.liveMotions.size, key = { work.liveMotions[it].key }) { index ->
                        val resource = work.liveMotions[index]
                        ResourceRow(
                            resource = resource,
                            coverUrl = work.coverUrl,
                            selected = resource.key in selected,
                            onToggle = { onToggle(resource) },
                            onPreview = { previewVideo = resource }
                        )
                    }
                    item { LivePhotoNote() }
                }
                item { SaveHint() }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            GlassBorderSurface(modifier = Modifier.fillMaxWidth(), corner = 24.dp, color = Color.White.copy(alpha = 0.96f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "已选 " + chosen.size + " 项",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (chosenBytes > 0) formatSize(chosenBytes) + " · 保存到媒体库" else "勾选需要保存的文件",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted
                        )
                    }
                    if (selected.isEmpty()) {
                        TextButton(onClick = onSelectAll) { Text("全选") }
                    } else {
                        TextButton(onClick = onClearSelection) { Text("取消", color = Muted) }
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = onDownload,
                        enabled = chosen.isNotEmpty() && !saving,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue,
                            disabledContainerColor = Blue.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (saving) "加入中…" else "下载选中项", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    previewVideo?.let { resource ->
        MediaPreviewDialog(
            work = work,
            resource = resource,
            onDismiss = { previewVideo = null }
        )
    }
    if (previewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = previewImages.map { it.url },
            initialIndex = previewImageIndex,
            onDismiss = { previewImages = emptyList() }
        )
    }
}

@Composable
private fun ResultTopBar(work: ParsedWork, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.86f))
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "解析结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            PlatformBadge(work.platform)
        }
    }
}

@Composable
private fun WorkHeader(work: ParsedWork) {
    GlassCard(modifier = Modifier.fillMaxWidth(), corner = 26.dp, contentPadding = 14.dp) {
        Row {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(136.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                NetworkImage(
                    url = work.coverUrl,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 18,
                    maxDimension = 720
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = work.platform.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                if (!work.author.isNullOrBlank()) {
                    Text(
                        text = "@" + work.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
                if (work.isLivePhoto) {
                    Spacer(Modifier.height(9.dp))
                    Pill(
                        text = "抖音实况图",
                        color = Color(0xFF7E22CE),
                        background = Color(0xFFF3E8FF)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (work.normalVideos.isNotEmpty()) {
                            Pill("视频 " + work.normalVideos.size, color = Blue, background = BlueSoft)
                        }
                        if (work.images.isNotEmpty()) {
                            Pill("图片 " + work.images.size, color = Success, background = Color(0xFFE8F8EF))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (work.liveMotions.isNotEmpty()) {
                            Pill("实况 " + work.liveMotions.size, color = Color(0xFF7E22CE), background = Color(0xFFF3E8FF))
                        }
                        if (work.audios.isNotEmpty()) {
                            Pill("音频", color = Color(0xFF7E22CE), background = Color(0xFFF3E8FF))
                        }
                    }
                }
                val duration = formatDuration(work.durationMs)
                if (duration.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = "时长 " + duration, style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(
    resource: MediaResource,
    coverUrl: String?,
    selected: Boolean,
    onToggle: () -> Unit,
    onPreview: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onToggle),
        shape = shape,
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Blue else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
            ) {
                if (resource.kind == ResourceKind.VIDEO && coverUrl != null) {
                    NetworkImage(url = coverUrl, modifier = Modifier.fillMaxSize(), cornerRadius = 15, maxDimension = 240)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when (resource.kind) {
                                    ResourceKind.VIDEO -> BlueSoft
                                    ResourceKind.AUDIO -> Color(0xFFF3E8FF)
                                    ResourceKind.IMAGE -> Color(0xFFE8F8EF)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            kindIcon(resource.kind),
                            null,
                            tint = when (resource.kind) {
                                ResourceKind.VIDEO -> Blue
                                ResourceKind.AUDIO -> Color(0xFF7E22CE)
                                ResourceKind.IMAGE -> Success
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (resource.kind == ResourceKind.AUDIO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .background(Color(0x22FFFFFF))
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            resource.liveMotion -> resource.label
                            resource.kind == ResourceKind.VIDEO ->
                                if (resource.quality != null) "视频 · " + resource.quality else "视频"
                            resource.kind == ResourceKind.AUDIO -> "音频"
                            else -> resource.label
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (resource.kind == ResourceKind.VIDEO && resource.needMerge) {
                        Spacer(Modifier.width(6.dp))
                        Pill("自动合并", color = BlueDeep, background = BlueSoft)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (resource.liveMotion) {
                        "短 MP4 动态片段 · 可单独保存"
                    } else {
                        buildString {
                            val duration = formatDuration(resource.durationMs)
                            if (duration.isNotEmpty()) append(duration)
                            val size = formatSize(resource.sizeBytes)
                            if (size.isNotEmpty()) {
                                if (isNotEmpty()) append(" · ")
                                append(size)
                            }
                            if (isEmpty()) append(if (resource.kind == ResourceKind.AUDIO) "M4A" else "原画")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
            TextButton(
                onClick = onPreview,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp), tint = Blue)
                Spacer(Modifier.width(2.dp))
                Text("预览", style = MaterialTheme.typography.labelSmall, color = Blue)
            }
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = Blue,
                    checkmarkColor = Color.White,
                    uncheckedColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}

@Composable
private fun ImageGrid(
    images: List<MediaResource>,
    selected: Set<String>,
    onToggle: (MediaResource) -> Unit,
    onPreview: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        images.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { colIndex, image ->
                    val index = rowIndex * 3 + colIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPreview(index) }
                    ) {
                        NetworkImage(
                            url = image.url,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 16,
                            maxDimension = 540
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(
                                    if (image.key in selected) Blue else Color.Black.copy(alpha = 0.35f)
                                )
                                .clickable { onToggle(image) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (image.key in selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "已选择",
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        Text(
                            text = (index + 1).toString(),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        if (image.liveVideoUrl != null) {
                            Pill(
                                text = "实况",
                                color = Color.White,
                                background = Color.Black.copy(alpha = 0.42f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(7.dp)
                            )
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ExtractAudioRow(checked: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFF3E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF7E22CE),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "同时提取视频原声",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "下载后额外生成一个 M4A 音频文件",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Blue
                )
            )
        }
    }
}

@Composable
private fun LivePhotoNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F5FF))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.Movie,
            contentDescription = null,
            tint = Color(0xFF7E22CE),
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "这里是可选的原生 MP4 动态短片。勾选上方实况图时，App 会把封面和短片合成为一个安卓 Motion Photo（_MP.jpg），相册可直接播放动态；如需单独剪辑，再下载这里的原始 MP4。",
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SaveHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CloudDownload, null, tint = Blue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "文件会保存到系统媒体库，App 内可在「我的 → 本地文件」管理。",
            style = MaterialTheme.typography.labelSmall,
            color = Muted
        )
    }
}