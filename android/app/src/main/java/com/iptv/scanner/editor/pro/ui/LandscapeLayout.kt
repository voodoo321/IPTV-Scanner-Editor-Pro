package com.iptv.scanner.editor.pro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.scanner.editor.pro.data.IptvChannel
import com.iptv.scanner.editor.pro.data.IptvEpgProgram
import com.iptv.scanner.editor.pro.player.PlayMode
import com.iptv.scanner.editor.pro.player.ProgressHelper
import com.iptv.scanner.editor.pro.ui.AppViewModel.ChannelTab
import com.iptv.scanner.editor.pro.ui.theme.PlayerOverlayColors
import com.iptv.scanner.editor.pro.ui.theme.rememberPlayerOverlayColors
import com.iptv.scanner.editor.pro.ui.theme.tvFocusBorder
import kotlinx.coroutines.delay

/**
 * 横屏沉浸式布局：左侧半透明侧边栏 + 右侧全屏视频 + 底部悬浮控制栏
 *
 * 设计理念：
 * - 视频始终全屏，不被压缩或裁剪
 * - 左侧侧边栏可收起/展开，毛玻璃背景，频道/EPG 双 Tab 切换
 * - 底部悬浮控制栏：进度条 + 播放控制按钮，半透明背景
 * - 顶部悬浮信息栏：频道名 + 媒体信息，极简设计
 * - 点击视频区域切换侧边栏/控制栏可见性
 *
 * 类似 YouTube/Plex 的沉浸式体验，但保留 IPTV 专业的频道/EPG 功能
 */

private val SIDEBAR_WIDTH = 280.dp
private val CONTROL_BAR_HEIGHT = 52.dp
private val INFO_BAR_HEIGHT = 40.dp

enum class LandscapeSideTab { CHANNELS, EPG }

@Composable
fun LandscapePlayerLayout(
    viewModel: AppViewModel,
    primaryPlayer: @Composable () -> Unit,
    videoAspectRatio: Float
) {
    val oc = rememberPlayerOverlayColors()
    val sidebarVisible by viewModel.landscapeSidebarVisible.collectAsState()
    val controlsVisible by viewModel.controlsVisible.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val paused by viewModel.mpv.paused.collectAsState()
    val muted by viewModel.mpv.muted.collectAsState()
    val volume by viewModel.mpv.volume.collectAsState()
    val fileLoaded by viewModel.mpv.fileLoaded.collectAsState()
    val videoWidth by viewModel.mpv.videoWidth.collectAsState()
    val videoHeight by viewModel.mpv.videoHeight.collectAsState()
    val currentIdx by viewModel.currentIdx.collectAsState()
    val showExitCatchup by viewModel.showExitCatchup.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var sideTab by remember { mutableStateOf(LandscapeSideTab.CHANNELS) }

    LaunchedEffect(Unit) {
        viewModel.setLandscapeSidebarVisible(true)
    }

    val anyOverlayOpen by derivedStateOf {
        sidebarVisible || controlsVisible
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            primaryPlayer()
        }

        if (!anyOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.setLandscapeSidebarVisible(true) }
            )
        }

        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            LandscapeSideBar(viewModel = viewModel, sideTab = sideTab, onTabChange = { sideTab = it })
        }

        if (sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = SIDEBAR_WIDTH)
                    .clickable { viewModel.setLandscapeSidebarVisible(false) }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible || sidebarVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            LandscapeTopBar(
                viewModel = viewModel,
                channelName = currentChannel?.name ?: "未选择频道",
                paused = paused,
                sidebarVisible = sidebarVisible,
                onShowEpg = {
                    sideTab = LandscapeSideTab.EPG
                    viewModel.setLandscapeSidebarVisible(true)
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible || sidebarVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            LandscapeBottomBar(
                viewModel = viewModel,
                paused = paused,
                muted = muted,
                volume = volume,
                fileLoaded = fileLoaded,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                showExitCatchup = showExitCatchup,
                playbackMode = playbackState.mode
            )
        }
    }
}

@Composable
private fun LandscapeSideBar(
    viewModel: AppViewModel,
    sideTab: LandscapeSideTab,
    onTabChange: (LandscapeSideTab) -> Unit
) {
    val oc = rememberPlayerOverlayColors()

    Surface(
        color = oc.topBarBg.copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxHeight()
            .width(SIDEBAR_WIDTH)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = sideTab == LandscapeSideTab.CHANNELS,
                    onClick = { onTabChange(LandscapeSideTab.CHANNELS) },
                    label = { Text("频道", fontSize = 12.sp) },
                    modifier = Modifier.tvFocusBorder()
                )
                FilterChip(
                    selected = sideTab == LandscapeSideTab.EPG,
                    onClick = { onTabChange(LandscapeSideTab.EPG) },
                    label = { Text("节目单", fontSize = 12.sp) },
                    modifier = Modifier.tvFocusBorder()
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { viewModel.setLandscapeSidebarVisible(false) },
                    modifier = Modifier.size(32.dp).tvFocusBorder()
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭侧边栏",
                        tint = oc.iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(oc.divider)
            )

            when (sideTab) {
                LandscapeSideTab.CHANNELS -> LandscapeChannelList(viewModel = viewModel)
                LandscapeSideTab.EPG -> LandscapeEpgList(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LandscapeChannelList(viewModel: AppViewModel) {
    val oc = rememberPlayerOverlayColors()
    val channels by viewModel.channels.collectAsState()
    val currentIdx by viewModel.currentIdx.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    val tab by viewModel.channelsTab.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val allGroups by viewModel.groups.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val groups = remember(allGroups, channels, tab) {
        if (tab == ChannelTab.LOCAL) {
            channels
                .filter { it.source.isEmpty() || ProgressHelper.isLocalFile(it.url) }
                .map { it.group }
                .filter { it.isNotEmpty() }
                .distinct()
        } else {
            allGroups
        }
    }

    val filteredChannels = remember(tab, searchQuery, selectedGroup, channels, favorites, history) {
        viewModel.getFilteredChannels().let { list ->
            if (searchQuery.isEmpty()) list else list.filter { (ch, _) ->
                ch.name.contains(searchQuery, ignoreCase = true) ||
                    ch.group.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChannelTab.values().take(4).forEach { t ->
                val label = when (t) {
                    ChannelTab.SUB -> "订阅"
                    ChannelTab.LOCAL -> "本地"
                    ChannelTab.FAV -> "收藏"
                    ChannelTab.HIST -> "历史"
                }
                val count = when (t) {
                    ChannelTab.SUB -> channels.size
                    ChannelTab.LOCAL -> channels.count { it.source.isEmpty() || ProgressHelper.isLocalFile(it.url) }
                    ChannelTab.FAV -> favorites.size
                    ChannelTab.HIST -> history.size
                }
                FilterChip(
                    selected = tab == t,
                    onClick = { viewModel.setChannelsTab(t) },
                    label = { Text("$label $count", fontSize = 10.sp, maxLines = 1) },
                    modifier = Modifier.tvFocusBorder()
                )
            }
        }

        Surface(
            color = oc.infoBarBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = oc.iconTint.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = oc.textPrimary,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("搜索频道", color = oc.textSecondary, fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                )
            }
        }

        if ((tab == ChannelTab.SUB || tab == ChannelTab.LOCAL) && groups.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Surface(
                        color = if (selectedGroup.isEmpty()) oc.accent.copy(alpha = 0.2f) else oc.infoBarBg,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { viewModel.setSelectedGroup("") }
                    ) {
                        Text(
                            "全部",
                            color = if (selectedGroup.isEmpty()) oc.accent else oc.textSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                items(groups.take(20)) { group ->
                    Surface(
                        color = if (selectedGroup == group) oc.accent.copy(alpha = 0.2f) else oc.infoBarBg,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { viewModel.setSelectedGroup(group) }
                    ) {
                        Text(
                            group,
                            color = if (selectedGroup == group) oc.accent else oc.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(filteredChannels) { (channel, idx) ->
                val isCurrent = idx == currentIdx
                val isFav = idx in favorites
                LandscapeChannelItem(
                    channel = channel,
                    isCurrent = isCurrent,
                    isFavorite = isFav,
                    oc = oc,
                    onClick = {
                        viewModel.playChannel(idx)
                        viewModel.setLandscapeSidebarVisible(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun LandscapeChannelItem(
    channel: IptvChannel,
    isCurrent: Boolean,
    isFavorite: Boolean,
    oc: PlayerOverlayColors,
    onClick: () -> Unit
) {
    val bg = if (isCurrent) oc.accent.copy(alpha = 0.18f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isCurrent) oc.accent else oc.iconTint.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = if (isCurrent) oc.textPrimary else oc.textSecondary,
                fontSize = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channel.group.isNotEmpty()) {
                Text(
                    text = channel.group,
                    color = oc.textSecondary.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
        if (isFavorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = oc.accent,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun LandscapeEpgList(viewModel: AppViewModel) {
    val oc = rememberPlayerOverlayColors()
    val epg by viewModel.currentEpg.collectAsState()
    val loading by viewModel.epgLoading.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentIdx by viewModel.currentIdx.collectAsState()

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    var epgDateOffset by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (epgDateOffset > -7) epgDateOffset-- },
                modifier = Modifier.size(28.dp).tvFocusBorder()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "前一天",
                    tint = if (epgDateOffset > -7) oc.iconTint else oc.iconTint.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = formatEpgDateLabel(epgDateOffset),
                color = if (epgDateOffset == 0) oc.accent else oc.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = { if (epgDateOffset < 7) epgDateOffset++ },
                modifier = Modifier.size(28.dp).tvFocusBorder()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "后一天",
                    tint = if (epgDateOffset < 7) oc.iconTint else oc.iconTint.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        when {
            currentIdx < 0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("请先选择频道", color = oc.textSecondary, fontSize = 12.sp)
                }
            }
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载节目单...", color = oc.textSecondary, fontSize = 12.sp)
                }
            }
            epg.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无节目数据", color = oc.textSecondary, fontSize = 12.sp)
                }
            }
            else -> {
                val cal = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_MONTH, epgDateOffset)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val dayStartMs = cal.timeInMillis
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                val dayEndMs = cal.timeInMillis

                val dateFiltered = epg.filter { p ->
                    val startMs = parseEpgTimeMs(p.start, p.startTs)
                    val endMs = parseEpgTimeMs(p.end.ifEmpty { p.stop }, p.stopTs)
                    startMs > 0 && endMs > startMs && startMs < dayEndMs && endMs > dayStartMs
                }

                if (dateFiltered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("该日期无节目数据", color = oc.textSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(dateFiltered) { program ->
                            val isCurrent = isEpgCurrent(program, now)
                            val isPast = isEpgPast(program, now)
                            LandscapeEpgItem(
                                program = program,
                                isCurrent = isCurrent,
                                isPast = isPast,
                                oc = oc,
                                onClick = {
                                    if (isPast && !isCurrent) {
                                        viewModel.startCatchup(program)
                                    } else {
                                        viewModel.toggleReminder(program, currentChannel)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeEpgItem(
    program: IptvEpgProgram,
    isCurrent: Boolean,
    isPast: Boolean,
    oc: PlayerOverlayColors,
    onClick: () -> Unit
) {
    val bg = if (isCurrent) oc.accent.copy(alpha = 0.15f) else Color.Transparent
    val alpha = if (isPast && !isCurrent) 0.5f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(bg, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        val timeText = buildString {
            append(formatEpgTime(program.start))
            if (program.stop.isNotEmpty() || program.end.isNotEmpty()) {
                append("-")
                append(formatEpgTime(program.stop.ifEmpty { program.end }))
            }
        }
        Text(
            text = timeText,
            color = if (isCurrent) oc.accent else oc.textSecondary,
            fontSize = 10.sp,
            modifier = Modifier.width(72.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = program.title,
                color = if (isCurrent) oc.textPrimary else oc.textSecondary,
                fontSize = 11.sp,
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            if (isCurrent) {
                Surface(
                    color = oc.accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Text(
                        text = "正在播出",
                        color = oc.accent,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeTopBar(
    viewModel: AppViewModel,
    channelName: String,
    paused: Boolean,
    sidebarVisible: Boolean,
    onShowEpg: () -> Unit
) {
    val oc = rememberPlayerOverlayColors()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val currentIdx by viewModel.currentIdx.collectAsState()
    val isFav = currentIdx in favorites

    Surface(
        color = oc.topBarBg.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(INFO_BAR_HEIGHT)
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!sidebarVisible) {
                IconButton(
                    onClick = { viewModel.setLandscapeSidebarVisible(true) },
                    modifier = Modifier.size(36.dp).tvFocusBorder()
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = "频道",
                        tint = oc.iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = channelName,
                color = oc.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (paused) {
                Text(
                    text = "已暂停",
                    color = oc.iconTintActive,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            if (currentChannel != null) {
                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.size(32.dp).tvFocusBorder()
                ) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFav) oc.accent else oc.iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onShowEpg,
                modifier = Modifier.size(32.dp).tvFocusBorder()
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "节目单",
                    tint = oc.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { viewModel.showMenuPanel() },
                modifier = Modifier.size(32.dp).tvFocusBorder()
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = oc.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LandscapeBottomBar(
    viewModel: AppViewModel,
    paused: Boolean,
    muted: Boolean,
    volume: Int,
    fileLoaded: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    showExitCatchup: Boolean,
    playbackMode: PlayMode
) {
    val oc = rememberPlayerOverlayColors()

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val progressInfo = remember(tick) { viewModel.computeProgress() }

    Surface(
        color = oc.topBarBg.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progressInfo.startLabel,
                    color = oc.textSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.width(40.dp)
                )
                Slider(
                    value = progressInfo.percent / 100f,
                    onValueChange = { viewModel.seekProgress(it * 100f) },
                    modifier = Modifier.weight(1f).height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = oc.accent,
                        activeTrackColor = oc.accent,
                        inactiveTrackColor = oc.trackInactive
                    )
                )
                Text(
                    text = progressInfo.endLabel,
                    color = oc.textSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.width(40.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.prevChannel() }, modifier = Modifier.size(32.dp).tvFocusBorder()) {
                        Icon(Icons.Default.SkipPrevious, null, tint = oc.iconTint, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { viewModel.mpv.togglePause() }, modifier = Modifier.size(32.dp).tvFocusBorder()) {
                        Icon(
                            if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            null, tint = oc.iconTint, modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.stopPlay() }, modifier = Modifier.size(32.dp).tvFocusBorder()) {
                        Icon(Icons.Default.Stop, null, tint = oc.iconTint, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { viewModel.nextChannel() }, modifier = Modifier.size(32.dp).tvFocusBorder()) {
                        Icon(Icons.Default.SkipNext, null, tint = oc.iconTint, modifier = Modifier.size(18.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fileLoaded && videoWidth > 0) {
                        val resLabel = when {
                            videoWidth >= 3800 -> "4K"
                            videoWidth >= 1900 -> "1080P"
                            videoWidth >= 1200 -> "720P"
                            else -> "${videoWidth}x${videoHeight}"
                        }
                        Surface(
                            color = oc.badgeBg,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = resLabel,
                                color = oc.badgeText,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (showExitCatchup) {
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.exitCatchup() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("退出回看", color = oc.accent, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = { viewModel.mpv.toggleMute() }, modifier = Modifier.size(32.dp).tvFocusBorder()) {
                        Icon(
                            if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            null,
                            tint = if (muted) oc.iconTintActive else oc.iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { viewModel.mpv.setVolume(it.toInt()) },
                        modifier = Modifier.width(80.dp).height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = oc.accent,
                            activeTrackColor = oc.accent,
                            inactiveTrackColor = oc.trackInactive
                        )
                    )

                    IconButton(
                        onClick = { viewModel.togglePlayerSettings() },
                        modifier = Modifier.size(32.dp).tvFocusBorder()
                    ) {
                        Icon(Icons.Default.Settings, null, tint = oc.iconTint, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun formatEpgDateLabel(offset: Int): String {
    if (offset == 0) return "今天"
    if (offset == -1) return "昨天"
    if (offset == 1) return "明天"
    val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, offset) }
    val dateFmt = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
    val weekFmt = java.text.SimpleDateFormat("E", java.util.Locale.CHINESE)
    return "${dateFmt.format(cal.time)} ${weekFmt.format(cal.time)}"
}

private fun parseEpgTimeMs(iso: String, ts: Long): Long {
    if (ts > 0) return ts * 1000L
    if (iso.isEmpty()) return 0
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm"
    )
    for (pattern in patterns) {
        try {
            return java.text.SimpleDateFormat(pattern, java.util.Locale.US).parse(iso)?.time ?: continue
        } catch (_: Exception) {}
    }
    return 0
}

private fun formatEpgTime(iso: String): String {
    if (iso.isEmpty()) return ""
    val ms = parseEpgTimeMs(iso, 0)
    if (ms <= 0) return iso
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    return String.format(java.util.Locale.US, "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
}

private fun isEpgCurrent(program: IptvEpgProgram, nowMs: Long): Boolean {
    val startMs = parseEpgTimeMs(program.start, program.startTs)
    val endMs = parseEpgTimeMs(program.end.ifEmpty { program.stop }, program.stopTs)
    return startMs > 0 && endMs > startMs && nowMs >= startMs && nowMs < endMs
}

private fun isEpgPast(program: IptvEpgProgram, nowMs: Long): Boolean {
    val endMs = parseEpgTimeMs(program.end.ifEmpty { program.stop }, program.stopTs)
    return endMs > 0 && nowMs >= endMs
}