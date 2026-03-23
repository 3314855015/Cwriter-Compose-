package com.cwriter.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Volume
import com.cwriter.ui.viewmodel.VolumedWorkViewModel
import kotlinx.coroutines.launch

// ===== 主题色 =====
private val BgDrawerLight   = Color(0xFFFFFFFF)
private val BgDrawerDark    = Color(0xFF2D2D2D)
private val BgIndexLight    = Color(0xFFF5F5F5)
private val BgIndexDark     = Color(0xFF252525)
private val BgVolHeaderLight = Color(0xFFF0F0F0)
private val BgVolHeaderDark  = Color(0xFF333333)
private val TextMainLight   = Color(0xFF333333)
private val TextMainDark    = Color(0xFFFFFFFF)
private val TextSubLight    = Color(0xFF666666)
private val TextSubDark     = Color(0xFFB3B3B3)
private val TextHintLight   = Color(0xFFB3B3B3)
private val TextHintDark    = Color(0xFF808080)
private val DividerLight    = Color(0xFFEEEEEE)
private val DividerDark     = Color(0xFF404040)
private val Orange          = Color(0xFFFF6B35)
private val OrangeLightBg   = Color(0xFFFFF0EB)
private val OrangeDarkBg    = Color(0x26FF6B35)
private val Scrim           = Color(0x80000000)

/**
 * 目录面板 — 参考 UniApp CatalogPanel.vue
 *
 * 布局：左栏(1/3) 卷索引 + 右栏(2/3) 连续章节列表
 * 左栏点击卷名 → 右栏滚动到对应卷区域（scroll-into-view 效果）
 * 右栏：卷名作为 sticky header，章节列表连续展示
 */
@Composable
fun CatalogPanel(
    isVisible: Boolean,
    isDark: Boolean = false,
    workTitle: String = "",
    onDismiss: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    viewModel: VolumedWorkViewModel
) {
    val volumes         by viewModel.volumes.collectAsState()
    val chaptersByVolume by viewModel.chaptersByVolume.collectAsState()

    // 确保所有卷的章节都已加载
    LaunchedEffect(isVisible, volumes) {
        if (isVisible) {
            volumes.forEach { volume ->
                if (!viewModel.isVolumeLoaded(volume.id)) {
                    viewModel.loadVolumeChaptersAsync(volume.id)
                }
            }
        }
    }

    // 当前激活的卷（左栏高亮）
    var activeVolumeId by remember(volumes) {
        mutableStateOf(volumes.firstOrNull()?.id)
    }

    // 右栏 LazyList 状态，用于滚动定位
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 构建右栏的 item 索引表：volumeId → 在 LazyColumn 中的起始 index
    // 每个卷占 1 个 header item + N 个章节 item
    val volumeStartIndex = remember(volumes, chaptersByVolume) {
        val map = mutableMapOf<String, Int>()
        var idx = 0
        volumes.forEach { vol ->
            map[vol.id] = idx
            idx += 1 + (chaptersByVolume[vol.id]?.size ?: 0)
        }
        map
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val panelWidthDp  = (screenWidthDp * 3 / 4).dp

    val offsetX by animateDpAsState(
        targetValue    = if (isVisible) 0.dp else -panelWidthDp,
        animationSpec  = tween(durationMillis = 260),
        label          = "catalog_slide"
    )

    // 颜色
    val bgDrawer   = if (isDark) BgDrawerDark   else BgDrawerLight
    val bgIndex    = if (isDark) BgIndexDark    else BgIndexLight
    val bgVolHeader = if (isDark) BgVolHeaderDark else BgVolHeaderLight
    val textMain   = if (isDark) TextMainDark   else TextMainLight
    val textSub    = if (isDark) TextSubDark    else TextSubLight
    val textHint   = if (isDark) TextHintDark   else TextHintLight
    val divider    = if (isDark) DividerDark    else DividerLight
    val activeItemBg = if (isDark) OrangeDarkBg else OrangeLightBg

    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明遮罩
        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Scrim)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
            )
        }

        // 抽屉面板
        Box(
            modifier = Modifier
                .width(panelWidthDp)
                .fillMaxHeight()
                .offset(x = offsetX)
                .background(bgDrawer)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgDrawer)
                        .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "目录",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMain
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF404040) else Color(0xFFF0F0F0))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", fontSize = 16.sp, color = textSub, fontWeight = FontWeight.Light)
                    }
                }
                HorizontalDivider(color = divider, thickness = 0.5.dp)

                // 两栏内容
                Row(modifier = Modifier.fillMaxSize()) {

                    // ── 左栏：卷索引 (1/3) ──────────────────────────────
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(bgIndex),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        // 书名（不可点击，固定顶部）
                        if (workTitle.isNotEmpty()) {
                            item(key = "book_title") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0))
                                        .padding(horizontal = 12.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = workTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMain,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp
                                    )
                                }
                                HorizontalDivider(color = divider, thickness = 0.5.dp)
                            }
                        }

                        itemsIndexed(volumes) { _, volume ->
                            val isActive = volume.id == activeVolumeId
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isActive) activeItemBg else Color.Transparent)
                                    .then(
                                        if (isActive) Modifier.padding(start = 3.dp)
                                            .background(Orange.copy(alpha = 0f)) // 左边橙色竖线
                                        else Modifier
                                    )
                                    .clickable {
                                        activeVolumeId = volume.id
                                        // 滚动右栏到对应卷
                                        val targetIdx = volumeStartIndex[volume.id] ?: 0
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(targetIdx)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 14.dp)
                            ) {
                                // 左侧橙色竖线
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = (-12).dp)
                                            .width(3.dp)
                                            .height(20.dp)
                                            .background(Orange, RoundedCornerShape(1.5.dp))
                                    )
                                }
                                Text(
                                    text = volume.name.ifEmpty { volume.title.ifEmpty { "未命名卷" } },
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isActive) Orange else textSub,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // 分割线
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .fillMaxHeight()
                            .background(divider)
                    )

                    // ── 右栏：连续章节列表 (2/3) ─────────────────────────
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        volumes.forEach { volume ->
                            // 卷名 header（sticky 效果用 stickyHeader 需要 ExperimentalFoundationApi，
                            // 这里用普通 item 保持简洁，与 UniApp 一致）
                            item(key = "vol_${volume.id}") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bgVolHeader)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = volume.name.ifEmpty { volume.title.ifEmpty { "未命名卷" } },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Orange
                                    )
                                }
                                HorizontalDivider(color = divider, thickness = 0.5.dp)
                            }

                            val chapters = chaptersByVolume[volume.id] ?: emptyList()
                            if (chapters.isEmpty()) {
                                item(key = "empty_${volume.id}") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 16.dp)
                                    ) {
                                        Text("暂无章节", fontSize = 13.sp, color = textHint)
                                    }
                                }
                            } else {
                                // 计算该卷在全局的起始章节序号
                                val globalOffset = run {
                                    var offset = 0
                                    for (v in volumes) {
                                        if (v.id == volume.id) break
                                        offset += chaptersByVolume[v.id]?.size ?: 0
                                    }
                                    offset
                                }
                                itemsIndexed(
                                    items = chapters,
                                    key   = { _, ch -> "ch_${ch.id}" }
                                ) { idx, chapter ->
                                    val chapterNum = globalOffset + idx + 1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onOpenChapter(chapter)
                                                onDismiss()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "第${chapterNum}章",
                                            fontSize = 11.sp,
                                            color = textHint,
                                            modifier = Modifier.width(40.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = chapter.title.ifEmpty { "未命名章节" },
                                            fontSize = 13.sp,
                                            color = textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (idx < chapters.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = divider,
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
