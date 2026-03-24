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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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
                        chapter        = chapter,
                        workTitle      = work?.title ?: "",
                        editorState    = editorState,
                        focusRequester = focusRequester,
                        fontSize       = fontSize,
                        lineHeight     = lineHeight,
                        textColor      = contentTextColor,
                        scrollState    = scrollState,
                        keyboardHeight = keyboardHeight,
                        screenHeightPx = screenHeightPx,
                        density        = density,
                        onContentChange = { viewModel.updateContent(it) }
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

        // ── FAB（蓝色，仅 B 状态）
        AnimatedVisibility(
            visible  = editorState == EditorState.B,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 80.dp),
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
        NestedListPanel(isVisible = showNestedListPanel, onDismiss = { viewModel.toggleNestedListPanel() })
        GlossaryPanel(
            isVisible    = showGlossaryPanel,
            onDismiss    = { viewModel.toggleGlossaryPanel() },
            onInsertText = { text -> viewModel.updateContent((chapter?.content ?: "") + text) }
        )
        if (showForeshadowingPanel) {
            ForeshadowingBottomSheet(
                isVisible = true, paragraphIndex = 0,
                currentChapterId = chapterId, foreshadowings = emptyList(),
                onDismiss = { viewModel.toggleForeshadowingPanel() },
                onCreateForeshadowing = {}, onRecycleForeshadowing = {}, onUnrecycleForeshadowing = {}
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
    onContentChange: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val textStyle = TextStyle(
        fontSize   = fontSize.sp,
        lineHeight = (fontSize * lineHeight).sp,
        color      = textColor
    )
    val lineHeightPx = with(density) { (fontSize * lineHeight).sp.toPx() }
    var textFieldTopPx by remember { mutableStateOf(0f) }

    // TextFieldValue 本地状态：持有文字 + 光标/选区，不依赖外部 chapter.content 驱动
    // 仅在 chapter.content 与本地文字不一致时（外部写入，如加载/撤销）才同步
    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    val externalContent = chapter?.content ?: ""
    LaunchedEffect(externalContent) {
        if (tfv.text != externalContent) {
            // 外部内容变化（加载/撤销/重做）：更新文字，光标移到末尾
            tfv = TextFieldValue(externalContent,
                selection = androidx.compose.ui.text.TextRange(externalContent.length))
        }
    }

    val bottomPadding = if (editorState == EditorState.C)
        keyboardHeight + 52.dp + 40.dp
    else
        40.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (editorState == EditorState.A) Modifier.statusBarsPadding() else Modifier)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 章节标题（居中，三态均显示）
        Text(
            text       = chapter?.title ?: "",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = textColor,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.15f), thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // 作品名·字数（A/B 状态居中，C 状态隐藏）
        if (editorState != EditorState.C) {
            Text(
                text      = "$workTitle · ${chapter?.wordCount ?: 0}字",
                fontSize  = 13.sp,
                color     = textColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 内容
        if (editorState == EditorState.C) {
            // 覆盖 MaterialTheme primary 为蓝色，消除系统橙色选择手柄
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(primary = Blue)
            ) {
                BasicTextField(
                    value = tfv,
                    onValueChange = { newTfv ->
                        val oldText = tfv.text
                        val newText = newTfv.text

                        // 自动缩进：行数增加时处理
                        val processed = applyAutoIndent(oldText, newText)
                        if (processed != newText) {
                            // 缩进改变了文字：更新文字，光标移到新行缩进后
                            val extraChars = processed.length - newText.length
                            val newCursor = (newTfv.selection.end + extraChars).coerceIn(0, processed.length)
                            tfv = TextFieldValue(
                                text      = processed,
                                selection = androidx.compose.ui.text.TextRange(newCursor)
                            )
                            onContentChange(processed)
                        } else {
                            tfv = newTfv
                            onContentChange(newText)
                        }

                        // 换行时若光标在屏幕下半，自动滚动一行
                        if (keyboardHeight > 0.dp && screenHeightPx > 0
                            && textFieldTopPx > screenHeightPx / 2f) {
                            coroutineScope.launch { scrollState.animateScrollBy(lineHeightPx) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned { coords ->
                            textFieldTopPx = coords.positionInRoot().y
                        },
                    textStyle   = textStyle,
                    cursorBrush = SolidColor(Blue)
                )
            }
        } else {
            Text(
                text  = chapter?.content?.ifEmpty { "暂无内容..." } ?: "暂无内容...",
                style = textStyle
            )
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
                    PngToolSlot(com.cwriter.R.drawable.last,  "上一章",  enabled = hasPrevChapter, onClick = onPrevChapter)
                    Spacer(Modifier.weight(1f))
                    PngToolSlot(com.cwriter.R.drawable.read,  "阅读模式", active = readMode == ReadMode.PAGE, onClick = onToggleReadMode)
                    Spacer(Modifier.weight(1f))
                    PngToolSlot(
                        drawableId = if (localDarkMode) com.cwriter.R.drawable.light else com.cwriter.R.drawable.dark,
                        label      = if (localDarkMode) "浅色模式" else "深色模式",
                        onClick    = onToggleTheme
                    )
                    Spacer(Modifier.weight(1f))
                    PngToolSlot(com.cwriter.R.drawable.next,  "下一章",  enabled = hasNextChapter, onClick = onNextChapter)
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
