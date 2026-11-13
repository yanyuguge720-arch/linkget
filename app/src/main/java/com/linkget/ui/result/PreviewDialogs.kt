@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.linkget.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.model.Platform
import com.linkget.data.model.ResourceKind
import com.linkget.data.model.formatDuration
import com.linkget.ui.components.NetworkImage
import kotlinx.coroutines.delay

@Composable
fun MediaPreviewDialog(work: ParsedWork, resource: MediaResource, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val headers = remember(work.platform) {
        when (work.platform) {
            Platform.BILIBILI -> mapOf(
                "Referer" to "https://www.bilibili.com/",
                "Origin" to "https://www.bilibili.com"
            )
            Platform.DOUYIN -> mapOf("Referer" to "https://www.douyin.com/")
            Platform.TOUTIAO -> mapOf("Referer" to "https://www.toutiao.com/")
            else -> emptyMap()
        }
    }
    val player = remember(resource.url) {
        val dataSource = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(resource.url))
                prepare()
                playWhenReady = true
            }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(resource.durationMs ?: 0L) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            val currentDuration = player.duration
            if (currentDuration > 0L) duration = currentDuration
            delay(500L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B14))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                }
                Spacer(Modifier.padding(horizontal = 5.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (resource.kind == ResourceKind.AUDIO) "音频试听" else "视频预览",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = listOfNotNull(
                            resource.quality,
                            formatDuration(resource.durationMs).takeIf { it.isNotEmpty() }
                        ).joinToString(" · "),
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (resource.kind == ResourceKind.AUDIO) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .background(
                                Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(40.dp)
                            )
                            .clickable {
                                if (isPlaying) player.pause() else player.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = work.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = work.author?.let { "@" + it } ?: "背景音乐",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(22.dp))
                    val fraction = if (duration > 0) {
                        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth(0.74f)
                            .height(5.dp),
                        color = Color(0xFF60A5FA),
                        trackColor = Color.White.copy(alpha = 0.14f)
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.74f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(position).ifBlank { "00:00" },
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = if (isPlaying) "正在播放" else "已暂停",
                            color = Color(0xFF60A5FA),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = formatDuration(duration).ifBlank { "00:00" },
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                AndroidPlayerView(
                    player = player,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            Text(
                text = work.title,
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AndroidPlayerView(player: ExoPlayer, modifier: Modifier = Modifier) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { it.player = player },
        modifier = modifier
    )
}

@Composable
fun ImagePreviewDialog(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B14))
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                NetworkImage(
                    url = images[page],
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    maxDimension = 2400
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = (pagerState.currentPage + 1).toString() + " / " + images.size,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}