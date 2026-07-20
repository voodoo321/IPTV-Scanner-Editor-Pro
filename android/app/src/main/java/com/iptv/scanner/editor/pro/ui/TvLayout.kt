package com.iptv.scanner.editor.pro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.scanner.editor.pro.data.IptvChannel
import com.iptv.scanner.editor.pro.data.IptvEpgProgram
import com.iptv.scanner.editor.pro.player.PlayMode
import com.iptv.scanner.editor.pro.player.ProgressHelper
import com.iptv.scanner.editor.pro.ui.theme.rememberPlayerOverlayColors
import com.iptv.scanner.editor.pro.ui.theme.tvFocusBorder
import kotlinx.coroutines.delay

private val TV_SIDEBAR_WIDTH_EPG = 520.dp
private val TV_SIDEBAR_WIDTH_NO_EPG = 300.dp
private val TV_BOTTOM_BAR_HEIGHT = 80.dp
private val TV_ICON_SIZE = 26.dp
private val TV_ICON_BTN = 44.dp

@Composable
fun TvPlayerLayout(
    viewModel: AppViewModel,
    primaryPlayer: @Composable () -> Unit,
    videoAspectRatio: Float
) {
    val oc = rememberPlayerOverlayColors()
    val sidebarVisible by viewModel.landscapeSidebarVisible.collectAsState()
    val controlsVisible by viewModel.controlsVisible.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val paused by viewModel.mpv.paused.collectAsState()
    val fileLoaded by viewModel.mpv.fileLoaded.collectAsState()
    val videoWidth by viewModel.mpv.videoWidth.collectAsState()
    val videoHeight by viewModel.mpv.videoHeight.collectAsState()
    val showExitCatchup by viewModel.showExitCatchup.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentEpg by viewModel.currentEpg.collectAsState()
    val currentIdx by viewModel.currentIdx.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val currentProgram = remember(currentEpg) {
        ProgressHelper.findCurrentProgram(currentEpg, System.currentTimeMillis())
    }

    val hasEpg = currentEpg.isNotEmpty()
    val sidebarWidth = if (hasEpg) TV_SIDEBAR_WIDTH_EPG else TV_SIDEBAR_WIDTH_NO_EPG
    val isIdle = playbackState.mode == PlayMode.IDLE
    val showOverlays by derivedStateOf { sidebarVisible || controlsVisible }
    val showBottomBar = showOverlays && !isIdle

    Box(modifier = Modifier.fillMaxSize()) {
        primaryPlayer()

        if (!showOverlays) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.showControlsAutoHide() }
            )
        }

        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            TvSideBar(viewModel = viewModel, hasEpg = hasEpg, sidebarWidth = sidebarWidth)
        }

        if (sidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = sidebarWidth, bottom = if (showBottomBar) TV_BOTTOM_BAR_HEIGHT else 0.dp)
                    .clickable { viewModel.setLandscapeSidebarVisible(false) }
            )
        }

        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TvBottomBar(
                    viewModel = viewModel,
                    channel = currentChannel,
                    paused = paused,
                    fileLoaded = fileLoaded,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    showExitCatchup = showExitCatchup,
                    playbackMode = playbackState.mode,
                    currentProgram = currentProgram,
                    isFav = currentIdx in favorites
                )
            }
        }
    }
}

@Composable
private fun TvSideBar(viewModel: AppViewModel, hasEpg: Boolean, sidebarWidth: androidx.compose.ui.unit.Dp) {
    val oc = rememberPlayerOverlayColors()

    Surface(
        color = Color(0xDD1A1A2E),
        modifier = Modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .padding(bottom = TV_BOTTOM_BAR_HEIGHT)
    ) {
        if (hasEpg) {
            Row(modifier = Modifier.fillMaxSize()) {
                TvChannelColumn(viewModel = viewModel, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(oc.iconTint.copy(alpha = 0.15f)))
                TvEpgColumn(viewModel = viewModel, modifier = Modifier.weight(1f))
            }
        } else {
            TvChannelColumn(viewModel = viewModel, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TvChannelColumn(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val oc = rememberPlayerOverlayColors()
    val channelTab by viewModel.channelsTab.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    val currentIdx by viewModel.currentIdx.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val filteredChannels = remember(channelTab, channels, favorites, history) {
        viewModel.getFilteredChannels()
    }

    val listState = rememberLazyListState()
    val displayed: List<Pair<IptvChannel, Int>> = if (searchQuery.isBlank()) filteredChannels else filteredChannels.filter { (ch, _) -> ch.name.contains(searchQuery, true) }

    LaunchedEffect(currentIdx, displayed) {
        if (searchQuery.isBlank() && currentIdx >= 0) {
            val targetIndex = displayed.indexOfFirst { (_, idx) -> idx == currentIdx }
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex, scrollOffset = -listState.layoutInfo.viewportSize.height / 3)
            }
        }
    }

    Column(modifier = modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppViewModel.ChannelTab.values().forEach { tab ->
                FilterChip(
                    selected = channelTab == tab,
                    onClick = { viewModel.setChannelsTab(tab) },
                    label = {
                        Text(
                            when (tab) {
                                AppViewModel.ChannelTab.SUB -> "订阅"
                                AppViewModel.ChannelTab.LOCAL -> "本地"
                                AppViewModel.ChannelTab.FAV -> "收藏"
                                AppViewModel.ChannelTab.HIST -> "历史"
                            },
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.tvFocusBorder().height(36.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" },
                modifier = Modifier.size(36.dp).tvFocusBorder()
            ) {
                Icon(Icons.Default.Search, contentDescription = "搜索", tint = if (showSearch) oc.accent else oc.iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { viewModel.setLandscapeSidebarVisible(false) },
                modifier = Modifier.size(36.dp).tvFocusBorder()
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = oc.iconTint, modifier = Modifier.size(20.dp))
            }
        }

        if (showSearch) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索频道", fontSize = 13.sp, color = oc.textSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0x22FFFFFF), unfocusedContainerColor = Color(0x22FFFFFF), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = oc.textPrimary),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = oc.iconTint, modifier = Modifier.size(18.dp)) }
            )
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            itemsIndexed(displayed, key = { _, pair -> pair.second }) { _, (ch, idx) ->
                val isCurrent = idx == currentIdx
                val isFav = idx in favorites
                val canCatchup = ch.catchup.isNotEmpty() && ch.catchup != "none"
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .then(if (isCurrent) Modifier.background(oc.accent.copy(alpha = 0.2f)) else Modifier)
                        .clickable { viewModel.playChannel(idx) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ch.logo.isNotEmpty()) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(model = ch.logo, contentDescription = ch.name, modifier = Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(oc.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = oc.accent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = ch.name, color = if (isCurrent) oc.accent else oc.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (canCatchup) {
                        Icon(Icons.Default.History, contentDescription = "可回看", tint = oc.iconTintActive, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (isFav) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = oc.accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TvEpgColumn(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val oc = rememberPlayerOverlayColors()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val epgPrograms by viewModel.currentEpg.collectAsState()
    var dayOffset by remember { mutableStateOf(0) }

    val filteredPrograms = remember(epgPrograms, dayOffset) {
        if (dayOffset == 0) epgPrograms
        else {
            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, dayOffset) }
            val targetDay = cal.get(java.util.Calendar.YEAR) * 10000 + (cal.get(java.util.Calendar.MONTH) + 1) * 100 + cal.get(java.util.Calendar.DAY_OF_MONTH)
            epgPrograms.filter { prog ->
                val startMs = parseEpgTimeMs(prog.start, prog.startTs)
                if (startMs <= 0) return@filter false
                val startCal = java.util.Calendar.getInstance().apply { timeInMillis = startMs }
                val startDay = startCal.get(java.util.Calendar.YEAR) * 10000 + (startCal.get(java.util.Calendar.MONTH) + 1) * 100 + startCal.get(java.util.Calendar.DAY_OF_MONTH)
                startDay == targetDay
            }
        }
    }

    val epgListState = rememberLazyListState()

    LaunchedEffect(filteredPrograms) {
        if (dayOffset == 0 && filteredPrograms.isNotEmpty()) {
            val nowMs = System.currentTimeMillis()
            val currentIdx = filteredPrograms.indexOfFirst { isEpgCurrent(it, nowMs) }
            if (currentIdx >= 0) {
                epgListState.animateScrollToItem(currentIdx, scrollOffset = -epgListState.layoutInfo.viewportSize.height / 3)
            }
        }
    }

    Column(modifier = modifier.fillMaxHeight()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { dayOffset-- }, modifier = Modifier.size(36.dp).tvFocusBorder()) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = oc.iconTint, modifier = Modifier.size(20.dp)) }
            Text(text = formatEpgDateLabel(dayOffset), color = oc.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = { dayOffset++ }, modifier = Modifier.size(36.dp).tvFocusBorder()) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = oc.iconTint, modifier = Modifier.size(20.dp)) }
        }
        if (currentChannel == null) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("请先选择频道", color = oc.textSecondary, fontSize = 14.sp) } }
        else if (filteredPrograms.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (dayOffset == 0) "暂无节目单数据" else "该日无节目数据", color = oc.textSecondary, fontSize = 14.sp) } }
        else {
            val nowMs = System.currentTimeMillis()
            LazyColumn(state = epgListState, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(items = filteredPrograms, key = { prog -> prog.start + prog.title }) { prog ->
                    val isCurrent = isEpgCurrent(prog, nowMs)
                    val isPast = isEpgPast(prog, nowMs)
                    val canCatchup = isPast && currentChannel?.let { ch -> ch.catchup.isNotEmpty() && ch.catchup != "none" } == true
                    val rowBg = when {
                        isCurrent -> oc.accent.copy(alpha = 0.3f)
                        isPast -> Color(0x18FFFFFF)
                        else -> Color.Transparent
                    }
                    val timeColor = when { isCurrent -> oc.accent; isPast -> oc.textSecondary.copy(alpha = 0.4f); else -> oc.textPrimary }
                    val titleColor = when { isCurrent -> oc.accent; isPast -> oc.textSecondary.copy(alpha = 0.4f); else -> oc.textPrimary }
                    val titleStyle = when {
                        isCurrent -> androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.None)
                        isPast -> androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, textDecoration = TextDecoration.LineThrough)
                        else -> androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, textDecoration = TextDecoration.None)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(rowBg)
                            .then(if (canCatchup) Modifier.clickable { viewModel.startCatchup(prog) } else Modifier)
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrent) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(oc.accent))
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (isPast) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(oc.textSecondary.copy(alpha = 0.3f)))
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(text = formatEpgTime(prog.start), color = timeColor, fontSize = 13.sp, modifier = Modifier.width(50.dp))
                        Text(text = prog.title, color = titleColor, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (canCatchup) {
                            Icon(Icons.Default.History, contentDescription = "回看", tint = oc.accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvBottomBar(
    viewModel: AppViewModel,
    channel: IptvChannel?,
    paused: Boolean,
    fileLoaded: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    showExitCatchup: Boolean,
    playbackMode: PlayMode,
    currentProgram: IptvEpgProgram?,
    isFav: Boolean
) {
    val oc = rememberPlayerOverlayColors()

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { tick = System.currentTimeMillis(); delay(1000L) } }
    val progressInfo = remember(tick) { viewModel.computeProgress() }

    val mediaInfoBadges = if (fileLoaded) remember(tick, videoWidth, videoHeight) {
        buildTvMediaBadges(viewModel.mpv, videoWidth, videoHeight)
    } else emptyList()

    Surface(
        color = Color(0xDD1A1A2E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (channel != null && channel.logo.isNotEmpty()) {
                    AsyncImage(model = channel.logo, contentDescription = channel.name, modifier = Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = oc.accent, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel?.name?.ifEmpty { null } ?: "未选择频道",
                        color = if (channel != null) oc.textPrimary else oc.textSecondary,
                        fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (currentProgram != null && currentProgram.desc.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = currentProgram.desc, color = oc.textSecondary.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (showExitCatchup) {
                        Surface(color = oc.accent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(text = if (playbackMode == PlayMode.TIMESHIFT) "时移" else "回看", color = oc.accent, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (paused && fileLoaded) {
                        Surface(color = oc.badgeBg, shape = RoundedCornerShape(4.dp)) {
                            Text("已暂停", color = oc.badgeText, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    mediaInfoBadges.forEach { info ->
                        Surface(color = oc.badgeBg, shape = RoundedCornerShape(4.dp)) {
                            Text(text = info, color = oc.badgeText, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                if (currentProgram != null && currentProgram.title.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = currentProgram.title, color = oc.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (fileLoaded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = progressInfo.startLabel, color = oc.textSecondary, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                        Slider(
                            value = progressInfo.percent / 100f,
                            onValueChange = { viewModel.seekProgress(it * 100f) },
                            modifier = Modifier.weight(1f).height(24.dp),
                            colors = SliderDefaults.colors(thumbColor = oc.accent, activeTrackColor = oc.accent, inactiveTrackColor = oc.trackInactive)
                        )
                        Text(text = progressInfo.endLabel, color = oc.textSecondary, fontSize = 12.sp, modifier = Modifier.width(48.dp))

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(onClick = { viewModel.stopPlay() }, modifier = Modifier.size(TV_ICON_BTN).tvFocusBorder()) {
                            Icon(Icons.Default.Stop, "停止", tint = oc.iconTint, modifier = Modifier.size(TV_ICON_SIZE))
                        }
                        IconButton(onClick = { viewModel.setLandscapeSidebarVisible(true) }, modifier = Modifier.size(TV_ICON_BTN).tvFocusBorder()) {
                            Icon(Icons.Default.VideoLibrary, "频道列表", tint = oc.iconTint, modifier = Modifier.size(TV_ICON_SIZE))
                        }
                        IconButton(onClick = { viewModel.togglePlayerSettings() }, modifier = Modifier.size(TV_ICON_BTN).tvFocusBorder()) {
                            Icon(Icons.Default.Menu, "菜单", tint = oc.iconTint, modifier = Modifier.size(TV_ICON_SIZE))
                        }
                    }
                }
            }
        }
    }
}

private fun buildTvMediaBadges(mpv: com.iptv.scanner.editor.pro.player.Player, videoWidth: Int, videoHeight: Int): List<String> {
    val result = mutableListOf<String>()
    val mediaInfo = try { mpv.getMediaInfo() } catch (_: Exception) { emptyMap() }
    mediaInfo["videoCodec"]?.takeIf { it.isNotEmpty() && it != "null" }?.let { codec ->
        result.add(codec.removePrefix("video/").removePrefix("audio/").uppercase())
    }
    if (videoWidth > 0 && videoHeight > 0) {
        result.add(when { videoWidth >= 3800 -> "4K"; videoWidth >= 1900 -> "1080P"; videoWidth >= 1200 -> "720P"; else -> "${videoHeight}P" })
        result.add("${videoWidth}x${videoHeight}")
    }
    mediaInfo["audioCodec"]?.takeIf { it.isNotEmpty() && it != "null" }?.let { codec ->
        result.add(codec.removePrefix("audio/").uppercase())
    }
    mediaInfo["containerFormat"]?.takeIf { it.isNotEmpty() && it != "null" }?.let { result.add(it.take(10)) }
    mediaInfo["fps"]?.takeIf { it.isNotEmpty() && it != "null" && it != "0" && it != "0.000" }?.let { fps ->
        val fpsVal = fps.toFloatOrNull()
        result.add(if (fpsVal != null) "${fpsVal.toInt()}fps" else "${fps}fps")
    }
    mediaInfo["bitrate"]?.takeIf { it.isNotEmpty() && it != "null" && it != "0" }?.let { br ->
        val bps = br.toLongOrNull() ?: 0L
        if (bps > 0) result.add(if (bps > 1_000_000) "${bps / 1_000_000}Mbps" else "${bps / 1_000}Kbps")
    }
    mediaInfo["audioBitrate"]?.takeIf { it.isNotEmpty() && it != "null" && it != "0" }?.let { br ->
        val bps = br.toLongOrNull() ?: 0L
        if (bps > 0) result.add(if (bps > 1_000_000) "${bps / 1_000_000}Mbps" else "${bps / 1_000}Kbps")
    }
    return result.take(8)
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
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm")
    for (pattern in patterns) { try { return java.text.SimpleDateFormat(pattern, java.util.Locale.US).parse(iso)?.time ?: continue } catch (_: Exception) {} }
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