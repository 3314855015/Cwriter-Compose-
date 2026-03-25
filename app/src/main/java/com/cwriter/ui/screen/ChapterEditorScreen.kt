package com.cwriter.ui.screen

import android.view.ViewTreeObserver
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.Chapter
import com.cwriter.ui.components.ForeshadowingBottomSheet
import com.cwriter.ui.components.ForeshadowingOverlayPanel
import com.cwriter.ui.components.GlossaryPanel
import com.cwriter.ui.components.NestedListPanel
import com.cwriter.ui.theme.NavBarBackground
import com.cwriter.ui.viewmodel.ChapterEditorViewModel
import com.cwriter.ui.viewmodel.EditorState
import com.cwriter.ui.viewmodel.ReadMode
import kotlinx.coroutines.launch

private val Blue = Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditorScreen(
    userId: String,
    workId: String,
    chapterId: String,
    volumeId: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToChapter: ((String, String) -> Unit)? = null,
    viewModel: ChapterEditorViewModel = viewModel()
) {
    val context        = LocalContext.current
    val view           = LocalView.current
    val density        = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val work                   by viewModel.work.collectAsState()
    val chapter                by viewModel.chapter.collectAsState()
    val editorState            by viewModel.editorState.collectAsState()
    val isLoading              by viewModel.isLoading.collectAsState()
    val snackbarMessage        by viewModel.snackbarMessage.collectAsState()
    val readMode               by viewModel.readMode.collectAsState()
    val localDarkMode          by viewModel.localDarkMode.collectAsState()
    val fontSize               by viewModel.fontSize.collectAsState()
    val lineHeight             by viewModel.lineHeight.collectAsState()
    val showTextStylePanel     by viewModel.showTextStylePanel.collectAsState()
    val showNestedListPanel    by viewModel.showNestedListPanel.collectAsState()
    val showForeshadowingPanel by viewModel.showForeshadowingPanel.collectAsState()
    val showGlossaryPanel      by viewModel.showGlossaryPanel.collectAsState()
    val hasPrevChapter         by viewModel.hasPrevChapter.collectAsState()
    val hasNextChapter         by viewModel.hasNextChapter.collectAsState()
    val showCreateChapterDialog by viewModel.showCreateChapterDialog.collectAsState()
    val foreshadowings         by viewModel.foreshadowings.collectAsState()
    val selectedParagraphIndex by viewModel.selectedParagraphIndex.collectAsState()
    val showForeshadowingSheet by viewModel.showForeshadowingSheet.collectAsState()
    val allChapterPairs        by viewModel.allChapterPairsFlow.collectAsState()

    var keyboardHeight by remember { mutableStateOf(0.dp) }
    var screenHeightPx by remember { mutableStateOf(0) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            val imePx  = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            keyboardHeight = with(density) { imePx.toDp() }
            screenHeightPx = view.height
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }

    LaunchedEffect(userId, workId, chapterId) {
        viewModel.init(context, userId, workId, chapterId, volumeId)
    }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage.isNotEmpty()) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSnackbar()
        }
    }

    val focusManager   = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // B/C 共用同一个 ScrollState
    val scrollState = rememberScrollState()

    // 切换状态前先快照当前滚动值，切换后延迟恢复
    // 用 Int 而非 State，避免触发额外 recompose
    val pendingScrollRestore = remember { mutableStateOf(-1) }

    LaunchedEffect(editorState) {
        val target = pendingScrollRestore.value
        if (target >= 0) {
            // 等一帧让新布局（顶栏出现/消失）完成测量，再恢复滚动
            kotlinx.coroutines.delay(50)
            scrollState.scrollTo(target)
            pendingScrollRestore.value = -1
        }
        if (editorState == EditorState.C) {
            kotlinx.coroutines.delay(100)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val contentBg        = if (localDarkMode) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
    val contentTextColor = if (localDarkMode) Color(0xFFE0E0E0) else Color(0xFF333333)

    Box(modifier = Modifier.fillMaxSize().background(contentBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 顶部导航栏（B/C 状态）
            AnimatedVisibility(
                visible = editorState != EditorState.A,
                enter   = slideInVertically { -it } + fadeIn(),
                exit    = slideOutVertically { -it } + fadeOut()
            ) {
                EditorTopBar(
                    editorState     = editorState,
                    onEditClick     = {
                        pendingScrollRestore.value = scrollState.value
                        viewModel.enterEditMode()
                    },
                    onCompleteClick = {
                        pendingScrollRestore.value = scrollState.value
                        viewModel.exitEditMode()
                        focusManager.clearFocus()
                    },
                    onSaveClick = { viewModel.saveChapter() },
                    onSlotE = {
                        if (editorState == EditorState.C) viewModel.toggleNestedListPanel()
                        else viewModel.showSnackbar("请在编辑模式下使用故事线功能")
                    },
                    onSlotF = { viewModel.toggleForeshadowingPanel() },
                    onSlotG = {
                        if (editorState == EditorState.C) viewModel.toggleGlossaryPanel()
                        else viewModel.showSnackbar("请在编辑模式下使用写作板功能")
                    }
                )
            }

            // ── 主内容区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            when (editorState) {
                                EditorState.A -> viewModel.showToolbar()
                                EditorState.B -> viewModel.hideToolbar()
                                EditorState.C -> {}
                            }
                        })
                    }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    EditorContent(
                        chapter              = chapter,
                        workTitle            = work?.title ?: "",
                        editorState          = editorState,
                        focusRequester       = focusRequester,
                        fontSize             = fontSize,
                        lineHeight           = lineHeight,
                        textColor            = contentTextColor,
                        scrollState          = scrollState,
                        keyboardHeight       = keyboardHeight,
                        screenHeightPx       = screenHeightPx,
                        density              = density,
                        pendingInsertText    = viewModel.pendingInsertText.collectAsState().value,
                        onInsertConsumed     = { viewModel.clearPendingInsert() },
                        onContentChange      = { viewModel.updateContent(it) },
                        onTitleChange        = { viewModel.updateTitle(it) },
                        foreshadowings       = foreshadowings,
                        showForeshadowingOverlay = showForeshadowingPanel,
                        onParagraphIconClick = { idx -> viewModel.openForeshadowingSheet(idx) }
                    )
                }
            }

            // ── 底部工具栏（B/C 状态）
            AnimatedVisibility(
                visible = editorState != EditorState.A,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                EditorBottomBar(
                    editorState        = editorState,
                    wordCount          = chapter?.wordCount ?: 0,
                    keyboardHeight     = keyboardHeight,
                    readMode           = readMode,
                    localDarkMode      = localDarkMode,
                    fontSize           = fontSize,
                    lineHeight         = lineHeight,
                    showTextStylePanel = showTextStylePanel,
                    hasPrevChapter     = hasPrevChapter,
                    hasNextChapter     = hasNextChapter,
                    onPrevChapter      = {
                        pendingScrollRestore.value = 0
                        viewModel.navigateToPrevChapter { cid, vid -> onNavigateToChapter?.invoke(cid, vid) }
                    },
                    onNextChapter      = {
                        pendingScrollRestore.value = 0
                        viewModel.navigateToNextChapter { cid, vid -> onNavigateToChapter?.invoke(cid, vid) }
                    },
                    onToggleReadMode   = { viewModel.toggleReadMode() },
                    onToggleTheme      = { viewModel.toggleLocalTheme() },
                    onIndentClick      = { viewModel.autoIndent() },
                    onToggleTextStyle  = { viewModel.toggleTextStylePanel() },
                    onFontSizeChange   = { viewModel.setFontSize(it) },
                    onLineHeightChange = { viewModel.setLineHeight(it) }
                )
            }
        }

        // ── FAB（蓝色，仅 B 状态）— 悬浮在底栏上方
        AnimatedVisibility(
            visible  = editorState == EditorState.B,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 96.dp), // 72dp ≈ 底栏高度
            enter    = scaleIn() + fadeIn(),
            exit     = scaleOut() + fadeOut()
        ) {
            FloatingActionButton(
                onClick        = {
                    pendingScrollRestore.value = scrollState.value
                    viewModel.enterEditMode()
                },
                containerColor = Blue,
                shape          = CircleShape,
                modifier       = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White)
            }
        }

        // ── Snackbar
        AnimatedVisibility(
            visible  = snackbarMessage.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (editorState != EditorState.A) 80.dp else 24.dp),
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit  = slideOutVertically { it / 2 } + fadeOut()
        ) {
            Surface(
                shape    = RoundedCornerShape(8.dp),
                color    = Color(0xFF323232),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(snackbarMessage, color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }
        }

        // ── 功能面板
        NestedListPanel(
            isVisible = showNestedListPanel,
            workId    = workId,
            onDismiss = { viewModel.toggleNestedListPanel() }
        )
        GlossaryPanel(
            isVisible    = showGlossaryPanel,
            workId       = workId,
            onDismiss    = { viewModel.toggleGlossaryPanel() },
            onInsertText = { text -> viewModel.requestInsertText(text) }
        )
        // showForeshadowingPanel = 图标叠加层开关（在 EditorContent 内部处理）
        // showForeshadowingSheet = 点击图标后弹出的底部弹窗
        if (showForeshadowingSheet) {
            ForeshadowingBottomSheet(
                isVisible              = true,
                paragraphIndex         = selectedParagraphIndex,
                currentChapterId       = chapterId,
                foreshadowings         = foreshadowings,
                allChapterPairs        = allChapterPairs,
                onDismiss              = { viewModel.closeForeshadowingSheet() },
                onCreateForeshadowing  = { content -> viewModel.createForeshadowing(selectedParagraphIndex, content) },
                onRecycleForeshadowing = { id -> viewModel.recycleForeshadowing(id) },
                onUnrecycleForeshadowing = { id -> viewModel.unrecycleForeshadowing(id) }
            )
        }

        // ── 新建章节模态框（最后一章点下一章时弹出）
        if (showCreateChapterDialog) {
            CreateNextChapterDialog(
                onDismiss = { viewModel.dismissCreateChapterDialog() },
                onConfirm = { title ->
                    viewModel.createChapterAndNavigate(title) { cid, vid ->
                        onNavigateToChapter?.invoke(cid, vid)
                    }
                }
            )
        }
    }
}

// ─── 顶部导航栏（无涟漪）────────────────────────────────────────────────────
@Composable
fun EditorTopBar(
    editorState: EditorState,
    onEditClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSlotE: () -> Unit,
    onSlotF: () -> Unit,
    onSlotG: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = NavBarBackground) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A槽位：编辑 / 完成（无涟漪）
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .pointerInput(editorState) {
                        detectTapGestures(onTap = {
                            when (editorState) {
                                EditorState.B -> onEditClick()
                                EditorState.C -> onCompleteClick()
                                else -> {}
                            }
                        })
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text  = if (editorState == EditorState.C) "完成" else "编辑",
                    color = Color(0xFFE0E0E0)
                )
            }

            // B槽位：保存（仅C状态，无涟漪）
            AnimatedVisibility(visible = editorState == EditorState.C) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .pointerInput(Unit) { detectTapGestures(onTap = { onSaveClick() }) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("保存", color = Color(0xFFE0E0E0))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onSlotE, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(id = com.cwriter.R.drawable.e), "功能E",
                    tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onSlotF, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(id = com.cwriter.R.drawable.f), "功能F",
                    tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onSlotG, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(id = com.cwriter.R.drawable.g), "功能G",
                    tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ─── 内容区域 ─────────────────────────────────────────────────────────────────
// 使用 TextFieldValue 保持光标位置，避免每次 recompose 跳回顶部。
// 自动缩进：行数增加时给换行前那行 + 所有新增行补 "　　"（对照 uniapp）。
// 选择手柄颜色：用 MaterialTheme 覆盖 primary 为蓝色，消除橙色水滴。
@Composable
fun EditorContent(
    chapter: Chapter?,
    workTitle: String,
    editorState: EditorState,
    focusRequester: FocusRequester,
    fontSize: Float,
    lineHeight: Float,
    textColor: Color,
    scrollState: androidx.compose.foundation.ScrollState,
    keyboardHeight: Dp,
    screenHeightPx: Int,
    density: androidx.compose.ui.unit.Density,
    pendingInsertText: String?,
    onInsertConsumed: () -> Unit,
    onContentChange: (String) -> Unit,
    onTitleChange: (String) -> Unit = {},
    foreshadowings: List<com.cwriter.data.model.Foreshadowing> = emptyList(),
    showForeshadowingOverlay: Boolean = false,
    onParagraphIconClick: (Int) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val textStyle = TextStyle(
        fontSize   = fontSize.sp,
        lineHeight = (fontSize * lineHeight).sp,
        color      = textColor
    )
    val lineHeightPx = with(density) { (fontSize * lineHeight).sp.toPx() }
    var textFieldTopPx by remember { mutableStateOf(0f) }

    // 段落位置：key=段落索引, value=(topPx, bottomPx) 相对于内容区域
    val paragraphBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }

    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    val externalContent = chapter?.content ?: ""
    LaunchedEffect(externalContent) {
        if (tfv.text != externalContent) {
            tfv = TextFieldValue(externalContent,
                selection = androidx.compose.ui.text.TextRange(externalContent.length))
        }
    }

    LaunchedEffect(pendingInsertText) {
        val insertText = pendingInsertText ?: return@LaunchedEffect
        val cursor = tfv.selection.end.coerceIn(0, tfv.text.length)
        val before = tfv.text.substring(0, cursor)
        val after  = tfv.text.substring(cursor)
        val newText = before + insertText + after
        tfv = TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(cursor + insertText.length))
        onContentChange(newText)
        onInsertConsumed()
    }

    val bottomPadding = if (editorState == EditorState.C) keyboardHeight + 52.dp + 40.dp else 40.dp

    // 段落列表（B状态只读时用于测量位置）
    val paragraphs = remember(externalContent) {
        externalContent.split("\n").filter { it.isNotBlank() }
    }

    // 内容区域顶部偏移（用于段落坐标系对齐）
    var contentAreaTopPx by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (editorState == EditorState.A) Modifier.statusBarsPadding() else Modifier)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── 章节标题 ─────────────────────────────────────────────────────────
        if (editorState == EditorState.C) {
            // C状态：可编辑标题
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor     = Color.Transparent,
                    backgroundColor = Blue.copy(alpha = 0.3f)
                )
            ) {
                BasicTextField(
                    value       = chapter?.title ?: "",
                    onValueChange = { onTitleChange(it) },
                    textStyle   = TextStyle(
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = textColor,
                        textAlign  = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(Blue),
                    modifier    = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if ((chapter?.title ?: "").isEmpty()) {
                                Text("章节标题", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = 0.3f), textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth())
                            }
                            inner()
                        }
                    }
                )
            }
        } else {
            Text(
                text       = chapter?.title ?: "",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = textColor,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 作品名·字数（A/B 状态，C 状态隐藏，无分割线）
        if (editorState != EditorState.C) {
            Text(
                text      = "$workTitle · ${chapter?.wordCount ?: 0}字",
                fontSize  = 13.sp,
                color     = textColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 内容区（B状态：段落列表+伏笔图标叠加；C状态：编辑框）──────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    contentAreaTopPx = coords.positionInRoot().y
                }
        ) {
            if (editorState == EditorState.C) {
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor     = Color.Transparent,
                        backgroundColor = Blue.copy(alpha = 0.3f)
                    )
                ) {
                    BasicTextField(
                        value = tfv,
                        onValueChange = { newTfv ->
                            val oldText = tfv.text
                            val newText = newTfv.text
                            val processed = applyAutoIndent(oldText, newText)
                            if (processed != newText) {
                                val extraChars = processed.length - newText.length
                                val newCursor = (newTfv.selection.end + extraChars).coerceIn(0, processed.length)
                                tfv = TextFieldValue(processed, selection = androidx.compose.ui.text.TextRange(newCursor))
                                onContentChange(processed)
                            } else {
                                tfv = newTfv
                                onContentChange(newText)
                            }
                            if (keyboardHeight > 0.dp && screenHeightPx > 0 && textFieldTopPx > screenHeightPx / 2f) {
                                coroutineScope.launch { scrollState.animateScrollBy(lineHeightPx) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onGloballyPositioned { coords -> textFieldTopPx = coords.positionInRoot().y },
                        textStyle   = textStyle,
                        cursorBrush = SolidColor(Blue)
                    )
                }
            } else {
                // A/B 状态：分段显示，每段测量位置供伏笔图标定位
                // showForeshadowingOverlay 开启时右侧留 28dp 给图标列（24dp图标 + 4dp间距）
                val showForeshadowing = (editorState == EditorState.A || editorState == EditorState.B) && showForeshadowingOverlay
                // 段落间距 = lineHeight * fontSize / 2（模拟行距视觉效果）
                val paraSpacingDp = with(density) { (fontSize * (lineHeight - 1f) / 2f).sp.toDp() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = if (showForeshadowing) 12.dp else 0.dp)
                ) {
                    if (paragraphs.isEmpty()) {
                        Text("暂无内容...", style = textStyle.copy(color = textColor.copy(alpha = 0.4f)))
                    } else {
                        paragraphs.forEachIndexed { index, para ->
                            if (index > 0) Spacer(Modifier.height(paraSpacingDp))
                            Text(
                                text     = para,
                                style    = textStyle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coords ->
                                        // 存 px 值，后续转 dp 时用 density
                                        val topPx    = coords.positionInRoot().y - contentAreaTopPx
                                        val bottomPx = topPx + coords.size.height
                                        paragraphBounds[index] = topPx to bottomPx
                                    }
                            )
                        }
                    }
                }

                // 伏笔图标叠加层（A/B状态，且 showForeshadowingOverlay 开启时）
                if (showForeshadowing) {
                    val localDensity = density
                    ForeshadowingOverlayPanel(
                        foreshadowings   = foreshadowings,
                        paragraphBounds  = paragraphBounds.mapValues { (idx, tb) ->
                            // px → dp 转换，top/bottom 存 dp float 值
                            com.cwriter.ui.components.ParagraphBounds(
                                index  = idx,
                                top    = with(localDensity) { tb.first.toDp().value },
                                bottom = with(localDensity) { tb.second.toDp().value }
                            )
                        },
                        currentChapterId = chapter?.id ?: "",
                        isClickable      = true,
                        onIconClick      = onParagraphIconClick,
                        modifier         = Modifier.align(Alignment.TopEnd).width(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

/**
 * 自动缩进（对照 uniapp applyAutoIndentOnNewLine）
 * 仅在行数真实增加时触发：
 *   1. 给换行前那行（旧最后一行）补 "　　"（若无）
 *   2. 给所有新增行补 "　　"（若无）
 */
private fun applyAutoIndent(oldText: String, newText: String): String {
    val oldLines = oldText.split("\n")
    val newLines = newText.split("\n")
    if (newLines.size <= oldLines.size) return newText   // 非换行操作，不处理

    val result  = newLines.toMutableList()
    var changed = false

    // 1. 换行前那行
    val lastOldIdx = oldLines.size - 1
    if (lastOldIdx >= 0 && !result[lastOldIdx].startsWith("　　")) {
        result[lastOldIdx] = "　　" + result[lastOldIdx]
        changed = true
    }
    // 2. 所有新增行
    for (i in oldLines.size until newLines.size) {
        if (!result[i].startsWith("　　")) {
            result[i] = "　　" + result[i]
            changed = true
        }
    }
    return if (changed) result.joinToString("\n") else newText
}

// ─── 底部工具栏 ───────────────────────────────────────────────────────────────
@Composable
fun EditorBottomBar(
    editorState: EditorState,
    wordCount: Int,
    keyboardHeight: Dp,
    readMode: ReadMode,
    localDarkMode: Boolean,
    fontSize: Float,
    lineHeight: Float,
    showTextStylePanel: Boolean,
    hasPrevChapter: Boolean,
    hasNextChapter: Boolean,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onToggleReadMode: () -> Unit,
    onToggleTheme: () -> Unit,
    onIndentClick: () -> Unit,
    onToggleTextStyle: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = if (editorState == EditorState.C) -keyboardHeight else 0.dp)
    ) {
        AnimatedVisibility(
            visible = editorState == EditorState.C && showTextStylePanel,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut()
        ) {
            TextStylePanel(fontSize, lineHeight, onFontSizeChange, onLineHeightChange)
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = NavBarBackground) {
            when (editorState) {
                // B状态：7槽位 PNG（H留白I阅读J留白K主题L留白M上一章N下一章）
                EditorState.B -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // 上一章：始终可点击，没有上一章时由 ViewModel 显示 snackbar
                    PngToolSlot(com.cwriter.R.drawable.last, "上一章", enabled = true, onClick = onPrevChapter)
                    Spacer(Modifier.weight(1f))
                    PngToolSlot(com.cwriter.R.drawable.read,  "阅读模式", active = readMode == ReadMode.PAGE, onClick = onToggleReadMode)
                    Spacer(Modifier.weight(1f))
                    PngToolSlot(
                        drawableId = if (localDarkMode) com.cwriter.R.drawable.light else com.cwriter.R.drawable.dark,
                        label      = if (localDarkMode) "浅色模式" else "深色模式",
                        onClick    = onToggleTheme
                    )
                    Spacer(Modifier.weight(1f))
                    // 下一章：始终可点击，最后一章时由 ViewModel 弹新建对话框
                    PngToolSlot(
                        drawableId = com.cwriter.R.drawable.next,
                        label      = "下一章",
                        enabled    = true,
                        onClick    = onNextChapter
                    )
                }

                // C状态：字数 + → + T
                EditorState.C -> Row(
                    modifier = Modifier
                        .fillMaxWidth().height(48.dp)
                        .navigationBarsPadding().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("$wordCount 字", color = Color(0xFFE0E0E0).copy(alpha = 0.7f), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 缩进（→），无涟漪
                        Box(
                            modifier = Modifier
                                .size(36.dp).clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) { detectTapGestures(onTap = { onIndentClick() }) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("→", color = Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        // T 字体样式，无涟漪
                        Box(
                            modifier = Modifier
                                .size(36.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (showTextStylePanel) Blue.copy(alpha = 0.2f) else Color.Transparent)
                                .pointerInput(showTextStylePanel) { detectTapGestures(onTap = { onToggleTextStyle() }) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T",
                                color      = if (showTextStylePanel) Blue else Color(0xFFE0E0E0),
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

// ─── PNG 工具槽位（上图标 + 下文字）─────────────────────────────────────────
@Composable
fun PngToolSlot(
    drawableId: Int,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(if (active) Blue.copy(alpha = 0.15f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter            = painterResource(id = drawableId),
                contentDescription = label,
                tint               = Color.Unspecified,
                modifier           = Modifier.size(24.dp)
            )
        }
        Text(
            text     = label,
            color    = when { active -> Blue; enabled -> Color(0xFFB3B3B3); else -> Color(0xFF666666) },
            fontSize = 11.sp
        )
    }
}

// ─── 字体样式调节面板 ─────────────────────────────────────────────────────────
@Composable
fun TextStylePanel(
    fontSize: Float,
    lineHeight: Float,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF252525)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("字号", color = Color(0xFFB3B3B3), fontSize = 13.sp, modifier = Modifier.width(36.dp))
                Slider(
                    value = fontSize, onValueChange = onFontSizeChange,
                    valueRange = 12f..28f, steps = 7, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue, inactiveTrackColor = Color(0xFF444444))
                )
                Text("${fontSize.toInt()}", color = Color(0xFFE0E0E0), fontSize = 13.sp,
                    modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("行距", color = Color(0xFFB3B3B3), fontSize = 13.sp, modifier = Modifier.width(36.dp))
                Slider(
                    value = lineHeight, onValueChange = onLineHeightChange,
                    valueRange = 1.2f..3.0f, steps = 8, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue, inactiveTrackColor = Color(0xFF444444))
                )
                Text(String.format("%.1f", lineHeight), color = Color(0xFFE0E0E0), fontSize = 13.sp,
                    modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
            }
        }
    }
}

// ─── 新建章节模态框（最后一章点下一章时弹出）────────────────────────────────
/**
 * 简化版新建章节对话框：只输入标题，不选卷（默认当前卷）
 * 对应 UniApp 的 CreateChapterModal，但去掉卷选择
 */
@Composable
fun CreateNextChapterDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF252525)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("新建章节", color = Color(0xFFE0E0E0), fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("章节标题（可留空）", color = Color(0xFF666666)) },
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = Color(0xFFE0E0E0),
                        fontSize = 16.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Blue,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor          = Blue
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF888888))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(title) }) {
                        Text("创建", color = Blue)
                    }
                }
            }
        }
    }
}
