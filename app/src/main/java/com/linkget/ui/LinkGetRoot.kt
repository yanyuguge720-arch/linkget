package com.linkget.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linkget.di.AppContainer
import com.linkget.ui.downloads.DownloadsScreen
import com.linkget.ui.downloads.DownloadsViewModel
import com.linkget.ui.history.HistoryScreen
import com.linkget.ui.history.HistoryViewModel
import com.linkget.ui.parse.ParseScreen
import com.linkget.ui.parse.ParseViewModel
import com.linkget.ui.profile.ProfileScreen
import com.linkget.ui.profile.ProfileViewModel
import com.linkget.ui.theme.Blue
import com.linkget.ui.theme.BlueSoft
import com.linkget.ui.theme.Ink
import com.linkget.ui.theme.Muted
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    PARSE("解析", Icons.Rounded.Link),
    DOWNLOADS("下载", Icons.Rounded.Download),
    HISTORY("历史", Icons.Rounded.History),
    PROFILE("我的", Icons.Rounded.Person)
}

@Composable
fun LinkGetRoot(container: AppContainer) {
    val factory = remember(container) { AppViewModelFactory(container) }
    val parseViewModel: ParseViewModel = viewModel(factory = factory)
    val downloadsViewModel: DownloadsViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)

    val parseState by parseViewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val showNav = parseState.result == null

    LaunchedEffect(pagerState.currentPage) {
        if (Tab.entries[pagerState.currentPage] == Tab.PROFILE) {
            profileViewModel.refresh()
        }
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (Tab.entries[page]) {
                Tab.PARSE -> ParseScreen(
                    viewModel = parseViewModel,
                    onQueued = { count ->
                        Toast.makeText(context, "已加入 " + count + " 个下载任务", Toast.LENGTH_SHORT).show()
                        scope.launch { pagerState.animateScrollToPage(Tab.DOWNLOADS.ordinal) }
                    }
                )
                Tab.DOWNLOADS -> DownloadsScreen(downloadsViewModel)
                Tab.HISTORY -> HistoryScreen(
                    viewModel = historyViewModel,
                    onReparse = { url ->
                        scope.launch { pagerState.animateScrollToPage(Tab.PARSE.ordinal) }
                        parseViewModel.parseUrl(url)
                    }
                )
                Tab.PROFILE -> ProfileScreen(profileViewModel)
            }
        }

        AnimatedVisibility(
            visible = showNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            BottomNav(
                selected = pagerState.currentPage,
                onSelect = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.95f)),
            shadowElevation = 18.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    val active = selected == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (active) BlueSoft else Color.Transparent)
                            .clickable { onSelect(index) }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (active) Blue else Muted,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Blue else Muted,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    }
                    if (index != Tab.entries.lastIndex) Spacer(Modifier.width(2.dp))
                }
            }
        }
    }
}
