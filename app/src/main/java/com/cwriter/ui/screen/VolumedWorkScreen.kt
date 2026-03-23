package com.cwriter.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Volume
import com.cwriter.ui.components.CatalogPanel
import com.cwriter.ui.components.CreateChapterDialog
import com.cwriter.ui.components.CreateVolumeDialog
import com.cwriter.ui.components.RenameVolumeDialog
import com.cwriter.ui.components.VolumeActionMenu
import com.cwriter.ui.theme.CWriterTheme
import com.cwriter.ui.theme.LocalIsDark
import com.cwriter.ui.viewmodel.VolumedWorkViewModel

// ===== 主题色（亮/暗两套，来自 UniApp）=====
private val BgPageLight       = Color(0xFFF5F5F5)
private val BgPageDark        = Color(0xFF1A1A1A)
private val BgCardLight       = Color(0xFFFFFFFF)
private val BgCardDark        = Color(0xFF2D2D2D)
private val BgHeaderLight     = Color(0xFFFFFFFF)
private val BgHeaderDark      = Color(0xFF2D2D2D)
private val BgChapterLight    = Color(0xFFF5F5F5)
private val BgChapterDark     = Color(0xFF252525)
private val TextMainLight     = Color(0xFF333333)
private val TextMainDark      = Color(0xFFFFFFFF)
private val TextSubLight      = Color(0xFF666666)
private val TextSubDark       = Color(0xFFB3B3B3)
private val TextHintLight     = Color(0xFFB3B3B3)
private val TextHintDark      = Color(0xFF808080)
private val BorderLight       = Color(0xFFE0E0E0)
private val BorderDark        = Color(0xFF404040)
private val DividerLight      = Color(0xFFF0F0F0)
private val DividerDark       = Color(0xFF333333)
private val Orange            = Color(0xFFFF6B35)

@Composable
fun VolumedWorkScreen(
    userId: String,
    workId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String, String, String) -> Unit,
    viewModel: VolumedWorkViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current

    // 动态颜色
    val bgPage     = if (isDark) BgPageDark     else BgPageLight
    val bgCard     = if (isDark) BgCardDark     else BgCardLight
    val bgHeader   = if (isDark) BgHeaderDark   else BgHeaderLight
    val textMain   = if (isDark) TextMainDark   else TextMainLight
    val textSub    = if (isDark) TextSubDark    else TextSubLight
    val textHint   = if (isDark) TextHintDark   else TextHintLight
    val border     = if (isDark) BorderDark     else BorderLight
    val divider    = if (isDark) DividerDark    else DividerLight

    val workInfo        by viewModel.workInfo.collectAsState()
    val volumes         by viewModel.volumes.collectAsState()
    val chaptersByVolume by viewModel.chaptersByVolume.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val sortOrder       by viewModel.sortOrder.collectAsState()
    val expandedVolumeId by viewModel.expandedVolumeId.collectAsState()

    var showCatalog             by remember { mutableStateOf(false) }
    var showCreateChapterModal  by remember { mutableStateOf(false) }
    var showCreateVolumeModal   by remember { mutableStateOf(false) }
    var showRenameVolumeModal   by remember { mutableStateOf(false) }
    var showVolumeMenu          by remember { mutableStateOf(false) }
    var currentEditingVolume    by remember { mutableStateOf<Volume?>(null) }

    LaunchedEffect(userId, workId) {
        viewModel.init(context, userId, workId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VolumedWorkViewModel.Event.ShowToast -> { /* TODO: snackbar */ }
                is VolumedWorkViewModel.Event.NavigateToEditor -> {
                    onNavigateToEditor(event.workId, event.chapterId, event.volumeId)
                }
            }
        }
    }

    // 正序/倒序后的卷列表（卷顺序变化，但卷名不变）
    val displayVolumes = remember(volumes, sortOrder) {
        if (sortOrder == "desc") volumes.reversed() else volumes
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgPage)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VolumedTopBar(
                title       = workInfo.title.ifEmpty { "加载中..." },
                bgHeader    = bgHeader,
                textMain    = textMain,
                textSub     = textSub,
                border      = border,
                onMenuClick = { showCatalog = true },
                onCloseClick = onNavigateBack
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Orange)
                }
            } else {
                SectionHeader(
                    totalChapters = volumes.sumOf { chaptersByVolume[it.id]?.size ?: 0 },
                    sortOrder     = sortOrder,
                    textMain      = textMain,
                    textHint      = textHint,
                    onToggleSort  = { viewModel.toggleSortOrder() }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AddVolumeCell(onClick = { showCreateVolumeModal = true })
                    }

                    if (volumes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("还没有创建卷", color = textHint, fontSize = 15.sp)
                            }
                        }
                    } else {
                        // 用 displayVolumes（已按正/倒序排列），卷名和卷数据完全对应
                        items(displayVolumes, key = { it.id }) { volume ->
                            VolumeCard(
                                volume       = volume,
                                chapters     = chaptersByVolume[volume.id] ?: emptyList(),
                                isExpanded   = expandedVolumeId == volume.id,
                                isLoaded     = viewModel.isVolumeLoaded(volume.id),
                                sortOrder    = sortOrder,
                                bgCard       = bgCard,
                                bgChapter    = if (isDark) BgChapterDark else BgChapterLight,
                                textMain     = textMain,
                                textSub      = textSub,
                                textHint     = textHint,
                                divider      = divider,
                                getChapterNumber = { idx -> viewModel.getChapterNumber(volume.id, idx, sortOrder) },
                                isLastGlobally   = { chapterId -> viewModel.isLastChapterGlobally(volume.id, chapterId) },
                                formatTime   = { ts -> viewModel.formatTime(ts) },
                                onToggleExpand = { viewModel.toggleVolumeExpand(volume.id) },
                                onLongPress  = { currentEditingVolume = volume; showVolumeMenu = true },
                                onChapterClick = { chapter -> viewModel.openChapter(chapter) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (volumes.isEmpty()) showCreateVolumeModal = true
                else showCreateChapterModal = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = Orange,
            elevation = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Text("+", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Light)
        }

        // 目录抽屉
        if (showCatalog) {
            CatalogPanel(
                isVisible    = true,
                isDark       = isDark,
                onDismiss    = { showCatalog = false },
                onOpenChapter = { chapter -> viewModel.openChapter(chapter) },
                viewModel    = viewModel
            )
        }

        if (showVolumeMenu && currentEditingVolume != null) {
            VolumeActionMenu(
                onDismiss = { showVolumeMenu = false },
                onRename  = { showVolumeMenu = false; showRenameVolumeModal = true },
                onDelete  = { showVolumeMenu = false; viewModel.confirmDeleteVolume(currentEditingVolume!!) }
            )
        }

        if (showCreateVolumeModal) {
            CreateVolumeDialog(
                onDismiss = { showCreateVolumeModal = false },
                onConfirm = { name, desc -> showCreateVolumeModal = false; viewModel.createVolume(name, desc) }
            )
        }

        if (showCreateChapterModal) {
            CreateChapterDialog(
                volumes   = volumes,
                onDismiss = { showCreateChapterModal = false },
                onConfirm = { title, content, volumeId ->
                    showCreateChapterModal = false
                    viewModel.createChapter(title, content, volumeId)
                }
            )
        }

        if (showRenameVolumeModal && currentEditingVolume != null) {
            RenameVolumeDialog(
                currentName = currentEditingVolume!!.title,
                onDismiss   = { showRenameVolumeModal = false; currentEditingVolume = null },
                onConfirm   = { newName ->
                    showRenameVolumeModal = false
                    currentEditingVolume?.let { viewModel.renameVolume(it.id, newName) }
                    currentEditingVolume = null
                }
            )
        }
    }
}

// ─── 顶栏 ────────────────────────────────────────────────────────────────────

@Composable
private fun VolumedTopBar(
    title: String,
    bgHeader: Color,
    textMain: Color,
    textSub: Color,
    border: Color,
    onMenuClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgHeader)
            .padding(start = 4.dp, end = 4.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(textSub, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
        Text(
            text      = title,
            modifier  = Modifier.weight(1f),
            fontSize  = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color     = textMain,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
        TextButton(onClick = onCloseClick) {
            Text("X", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textSub)
        }
    }
    HorizontalDivider(color = border, thickness = 0.5.dp)
}

// ─── 标题行 ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    totalChapters: Int,
    sortOrder: String,
    textMain: Color,
    textHint: Color,
    onToggleSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("章节列表", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textMain)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 $totalChapters 章", fontSize = 13.sp, color = textHint)
            val arrowRotation by animateFloatAsState(
                targetValue = if (sortOrder == "desc") 180f else 0f,
                animationSpec = tween(200), label = "sort_arrow"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Orange.copy(alpha = 0.12f))
                    .border(1.dp, Orange.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onToggleSort)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (sortOrder == "asc") "正序" else "倒序",
                        fontSize = 13.sp, color = Orange, fontWeight = FontWeight.Medium
                    )
                    Text("↓", fontSize = 12.sp, color = Orange,
                        modifier = Modifier.rotate(arrowRotation))
                }
            }
        }
    }
}

// ─── 新增卷按钮 ───────────────────────────────────────────────────────────────

@Composable
private fun AddVolumeCell(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Orange.copy(alpha = 0.08f))
            .border(2.dp, Orange.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Orange.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 20.sp, color = Orange, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.width(12.dp))
            Text("新增卷", fontSize = 15.sp, color = Orange, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── 卷卡片 ──────────────────────────────────────────────────────────────────

@Composable
private fun VolumeCard(
    volume: Volume,
    chapters: List<Chapter>,
    isExpanded: Boolean,
    isLoaded: Boolean,
    sortOrder: String,
    bgCard: Color,
    bgChapter: Color,
    textMain: Color,
    textSub: Color,
    textHint: Color,
    divider: Color,
    getChapterNumber: (Int) -> Int,
    isLastGlobally: (String) -> Boolean,
    formatTime: (String) -> String,
    onToggleExpand: () -> Unit,
    onLongPress: () -> Unit,
    onChapterClick: (Chapter) -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(200), label = "volume_arrow"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgCard,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            // 卷头行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onToggleExpand() },
                            onLongPress = { onLongPress() }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    color = textHint,
                    modifier = Modifier.rotate(arrowRotation)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = volume.name.ifEmpty { volume.title.ifEmpty { "未命名卷" } },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textMain,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = "${chapters.size}章", fontSize = 13.sp, color = textHint)
            }

            if (isExpanded) {
                HorizontalDivider(color = divider, thickness = 0.5.dp)

                if (!isLoaded) {
                    // 新建卷已预标记为已加载，这里只有真正懒加载时才会出现
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Orange)
                    }
                } else if (chapters.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无章节", fontSize = 13.sp, color = textHint)
                    }
                } else {
                    // 倒序时反转章节列表，但序号由 getChapterNumber 根据 displayIndex 计算
                    val displayChapters = if (sortOrder == "desc") chapters.reversed() else chapters
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgChapter)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayChapters.forEachIndexed { displayIndex, chapter ->
                            ChapterRow(
                                chapter       = chapter,
                                chapterNumber = getChapterNumber(displayIndex),
                                timeText      = formatTime(chapter.updatedAt.toString()),
                                isWriting     = isLastGlobally(chapter.id),
                                textMain      = textMain,
                                textHint      = textHint,
                                onClick       = { onChapterClick(chapter) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 章节行 ──────────────────────────────────────────────────────────────────

@Composable
private fun ChapterRow(
    chapter: Chapter,
    chapterNumber: Int,
    timeText: String,
    isWriting: Boolean,       // true = 全局最后一章（写作中），false = 已完成
    textMain: Color,
    textHint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "第${chapterNumber}章 ${chapter.title.ifEmpty { "未命名章节" }}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${chapter.wordCount}字", fontSize = 12.sp, color = textHint)
                Text(timeText, fontSize = 12.sp, color = textHint)
            }
        }

        // 状态标签：只有全局最后一章显示橙色"写作中"，其余显示灰色"已完成"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isWriting) Orange else Color(0xFFCCCCCC)
                )
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (isWriting) "写作中" else "已完成",
                fontSize = 10.sp,
                color = if (isWriting) Color.White else Color(0xFF888888)
            )
        }
    }
}

