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
import com.cwriter.ui.theme.DarkPrimary
import com.cwriter.ui.theme.NavBarBackground
import com.cwriter.ui.viewmodel.ChapterEditorViewModel
import com.cwriter.ui.viewmodel.EditorState
import com.cwriter.ui.viewmodel.ReadMode
import kotlinx.coroutines.launch

private val CursorBlue = Color(0xFF2196F3)
private val FabBlue    = Color(0xFF2196F3)

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
    val context       = LocalContext.current
    val view          = LocalView.current
    val density       = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val work                by viewModel.work.collectAsState()
    val chapter             by viewModel.chapter.collectAsState()
    val editorState         by viewModel.editorState.collectAsState()
    val isLoading           by viewModel.isLoading.collectAsState()
    val snackbarMessage     by viewModel.snackbarMessage.collectAsState()
    val readMode            by viewModel.readMode.collectAsState()
    val localDarkMode       by viewModel.localDarkMode.collectAsState()
    val fontSize            by viewModel.fontSize.collectAsState()
    val lineHeight          by viewModel.lineHeight.collectAsState()
    val showTextStylePanel  by viewModel.showTextStylePanel.collectAsState()
    val showNestedListPanel by viewModel.showNestedListPanel.collectAsState()
    val showForeshadowingPanel by viewModel.showForeshadowingPanel.collectAsState()
    val showGlossaryPanel   by viewModel.showGlossaryPanel.collectAsState()
    val hasPrevChapter      by viewModel.hasPrevChapter.collectAsState()
    val hasNextChapter      by viewModel.hasNextChapter.collectAsState()

    var keyboardHeight  by remember { mutableStateOf(0.dp) }
    var screenHeightPx  by remember { mutableStateOf(0) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets  = ViewCompat.getRootWindowInsets(view)
            val imePx   = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
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

    // B/C 共用同一个 ScrollState，保证切换时位置连续
    val scrollState = rememberScrollState()
    var savedScrollValue by remember { mutableStateOf(0) }

    LaunchedEffect(editorState) {
        when (editorState) {
            EditorState.C -> {
                coroutineScope.launch { scrollState.scrollTo(savedScrollValue) }
                kotlinx.coroutines.delay(100)
                try { focusRequester.requestFocus() } catch (_: Exception) {}
            }
            EditorState.B -> {
                coroutineScope.launch { scrollState.scrollTo(savedScrollValue) }
            }
            else -> {}
        }
    }

    val contentBg        = if (localDarkMode) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
    val contentTextColor = if (localDarkMode) Color(0xFFE0E0E0) else Color(0xFF333333)

    Box(modifier = Modifier.fillMaxSize().background(contentBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 顶部导航栏（B/C 状态）──────────────────────────────────────
            AnimatedVisibility(
                visible = editorState != EditorState.A,
                enter   = slideInVertically { -it } + fadeIn(),
                exit    = slideOutVertically { -it } + fadeOut()
            ) {
                EditorTopBar(
                    editorState    = editorState,
                    onEditClick    = { savedScrollValue = scrollState.value; viewModel.enterEditMode() },
                    onCompleteClick = {
                        savedScrollValue = scrollState.value
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

            // ── 主内容区 ──────────────────────────────────────────────────
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
                        onContentChange = { viewModel.updateContent(it) },
                        onScrollSave    = { savedScrollValue = it }
                    )
                }
            }

            // ── 底部工具栏（B/C 状态）────────────────────────────────────
            AnimatedVisibility(
                visible = editorState != EditorState.A,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                EditorBottomBar(
                    editorState       = editorState,
                    wordCount         = chapter?.wordCount ?: 0,
                    keyboardHeight    = keyboardHeight,
                    readMode          = readMode,
                    localDarkMode     = localDarkMode,
                    fontSize          = fontSize,
                    lineHeight        = lineHeight,
                    showTextStylePanel = showTextStylePanel,
                    hasPrevChapter    = hasPrevChapter,
                    hasNextChapter    = hasNextChapter,
                    onPrevChapter     = {
                        savedScrollValue = 0
                        viewModel.navigateToPrevChapter { cid, vid -> onNavigateToChapter?.invoke(cid, vid) }
                    },
                    onNextChapter     = {
                        savedScrollValue = 0
                        viewModel.navigateToNextChapter { cid, vid -> onNavigateToChapter?.invoke(cid, vid) }
                    },
                    onToggleReadMode  = { viewModel.toggleReadMode() },
                    onToggleTheme     = { viewModel.toggleLocalTheme() },
                    onIndentClick     = { viewModel.autoIndent() },
                    onToggleTextStyle = { viewModel.toggleTextStylePanel() },
                    onFontSizeChange  = { viewModel.setFontSize(it) },
                    onLineHeightChange = { viewModel.setLineHeight(it) }
                )
            }
        }

        // ── FAB（蓝色，仅 B 状态）────────────────────────────────────────
        AnimatedVisibility(
            visible  = editorState == EditorState.B,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 80.dp),
            enter    = scaleIn() + fadeIn(),
            exit     = scaleOut() + fadeOut()
        ) {
            FloatingActionButton(
                onClick        = { savedScrollValue = scrollState.value; viewModel.enterEditMode() },
                containerColor = FabBlue,
                shape          = CircleShape,
                modifier       = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White)
            }
        }

        // ── Snackbar ─────────────────────────────────────────────────────
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
                Text(
                    snackbarMessage,
                    color    = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        // ── 功能面板 ──────────────────────────────────────────────────────
        NestedListPanel(isVisible = showNestedListPanel, onDismiss = { viewModel.toggleNestedListPanel() })

        GlossaryPanel(
            isVisible    = showGlossaryPanel,
            onDismiss    = { viewModel.toggleGlossaryPanel() },
            onInsertText = { text -> viewModel.updateContent((chapter?.content ?: "") + text) }
        )

        if (showForeshadowingPanel) {
            ForeshadowingBottomSheet(
                isVisible              = true,
                paragraphIndex         = 0,
                currentChapterId       = chapterId,
                foreshadowings         = emptyList(),
                onDismiss              = { viewModel.toggleForeshadowingPanel() },
                onCreateForeshadowing  = {},
                onRecycleForeshadowing = {},
                onUnrecycleForeshadowing = {}
            )
        }
    }
}

// ─── 顶部导航栏 ──────────────────────────────────────────────────────────────
// 编辑/完成/保存 按钮无涟漪效果（indication = null）
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
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onSaveClick() })
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("保存", color = Color(0xFFE0E0E0))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // E/F/G 图标
            IconButton(onClick = onSlotE, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(id = com.cwriter.R.drawable.e),
                    contentDescription = "功能E",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onSlotF, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(id = com.cwriter.R.drawable.f),
                    contentDescription = "功能F",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onSlotG, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(id = com.cwriter.R.drawable.g),
                    contentDescription = "功能G",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─── 内容区域 ─────────────────────────────────────────────────────────────────
// 键盘弹出 + 换行时：若光标 Y > 屏幕 1/2，自动向下滚动一行高度
// C 状态换行自动缩进（两个全角空格）
// 底部 padding = keyboardHeight + 底栏高度，确保最后一行始终可见
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
    onContentChange: (String) -> Unit,
    onScrollSave: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val textStyle = TextStyle(
        fontSize   = fontSize.sp,
        lineHeight = (fontSize * lineHeight).sp,
        color      = textColor
    )
    val lineHeightPx = with(density) { (fontSize * lineHeight).sp.toPx() }

    // 记录 TextField 在根坐标系中的 Y 位置（用于判断是否在屏幕下半）
    var textFieldTopPx by remember { mutableStateOf(0f) }

    // 底部安全留白 = 键盘高度 + 底栏高度（52dp）+ 额外余量
    val bottomPadding = if (editorState == EditorState.C)
        keyboardHeight + 52.dp + 32.dp
    else
        40.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 章节标题（居中，A/B/C 均显示）
        if (editorState == EditorState.C) {
            BasicTextField(
                value       = chapter?.title ?: "",
                onValueChange = { /* 标题编辑后续扩展 */ },
                modifier    = Modifier.fillMaxWidth(),
                textStyle   = TextStyle(
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = textColor,
                    textAlign  = TextAlign.Center
                ),
                cursorBrush = SolidColor(CursorBlue),
                singleLine  = true
            )
        } else {
            Text(
                text      = chapter?.title ?: "",
                fontSize  = 22.sp,
                fontWeight = FontWeight.Bold,
                color     = textColor,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.15f), thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // 字数（A/B 状态）
        if (editorState != EditorState.C) {
            Text(
                text     = "$workTitle · ${chapter?.wordCount ?: 0}字",
                fontSize = 13.sp,
                color    = textColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 内容
        if (editorState == EditorState.C) {
            BasicTextField(
                value = chapter?.content ?: "",
                onValueChange = { newText ->
                    val old = chapter?.content ?: ""
                    // 换行自动缩进：检测到新增了 '\n'，在换行后插入两个全角空格
                    val processed = if (newText.length > old.length) {
                        val added = newText.substring(old.length)
                        if (added.contains('\n')) {
                            // 找到所有新增换行，在其后插入缩进
                            newText.replace(Regex("\n(?!　　)")) { "\n　　" }
                        } else newText
                    } else newText

                    onContentChange(processed)

                    // 键盘弹出时，若 TextField 在屏幕下半，自动滚动一行
                    if (keyboardHeight > 0.dp && screenHeightPx > 0) {
                        val halfScreen = screenHeightPx / 2f
                        if (textFieldTopPx > halfScreen) {
                            coroutineScope.launch {
                                scrollState.animateScrollBy(lineHeightPx)
                            }
                        }
                    }
                    onScrollSave(scrollState.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned { coords ->
                        textFieldTopPx = coords.positionInRoot().y
                    },
                textStyle   = textStyle,
                cursorBrush = SolidColor(CursorBlue)
            )
        } else {
            Text(
                text  = chapter?.content?.ifEmpty { "暂无内容..." } ?: "暂无内容...",
                style = textStyle
            )
        }

        // 动态底部留白：确保键盘弹出时最后一行不被遮挡
        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

// ─── 底部工具栏 ───────────────────────────────────────────────────────────────
// B状态：7槽位（H上一章 / I留白 / J阅读模式 / K留白 / L主题 / M留白 / N下一章）
//        图标用 PNG（last/read/light|dark/next），上图标+下文字
// C状态：字数 + 缩进(→) + T字体样式（蓝色，无撤销/重做）
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
        // TextStylePanel（C状态，底栏上方展开）
        AnimatedVisibility(
            visible = editorState == EditorState.C && showTextStylePanel,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut()
        ) {
            TextStylePanel(
                fontSize         = fontSize,
                lineHeight       = lineHeight,
                onFontSizeChange = onFontSizeChange,
                onLineHeightChange = onLineHeightChange
            )
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = NavBarBackground) {
            when (editorState) {
                // ── B状态：7槽位 PNG 图标 ──────────────────────────────────
                EditorState.B -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // H：上一章
                        PngToolSlot(
                            drawableId = com.cwriter.R.drawable.last,
                            label      = "上一章",
                            enabled    = hasPrevChapter,
                            onClick    = onPrevChapter
                        )
                        // I：留白
                        Spacer(modifier = Modifier.weight(1f))
                        // J：阅读模式
                        PngToolSlot(
                            drawableId = com.cwriter.R.drawable.read,
                            label      = "阅读模式",
                            active     = readMode == ReadMode.PAGE,
                            onClick    = onToggleReadMode
                        )
                        // K：留白
                        Spacer(modifier = Modifier.weight(1f))
                        // L：主题切换（light/dark PNG）
                        PngToolSlot(
                            drawableId = if (localDarkMode) com.cwriter.R.drawable.light else com.cwriter.R.drawable.dark,
                            label      = if (localDarkMode) "浅色模式" else "深色模式",
                            onClick    = onToggleTheme
                        )
                        // M：留白
                        Spacer(modifier = Modifier.weight(1f))
                        // N：下一章
                        PngToolSlot(
                            drawableId = com.cwriter.R.drawable.next,
                            label      = "下一章",
                            enabled    = hasNextChapter,
                            onClick    = onNextChapter
                        )
                    }
                }

                // ── C状态：字数 + 缩进 + T ────────────────────────────────
                EditorState.C -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "$wordCount 字",
                            color = Color(0xFFE0E0E0).copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // 缩进（→）
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { onIndentClick() })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("→", color = CursorBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            // T 字体样式
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (showTextStylePanel) CursorBlue.copy(alpha = 0.2f) else Color.Transparent)
                                    .pointerInput(showTextStylePanel) {
                                        detectTapGestures(onTap = { onToggleTextStyle() })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "T",
                                    color      = if (showTextStylePanel) CursorBlue else Color(0xFFE0E0E0),
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

// ─── PNG 工具槽位（B状态底栏，上图标+下文字）────────────────────────────────
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
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) CursorBlue.copy(alpha = 0.15f) else Color.Transparent),
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
            color    = if (active) CursorBlue
                       else if (enabled) Color(0xFFB3B3B3)
                       else Color(0xFF666666),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("字号", color = Color(0xFFB3B3B3), fontSize = 13.sp, modifier = Modifier.width(36.dp))
                Slider(
                    value        = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange   = 12f..28f,
                    steps        = 7,
                    modifier     = Modifier.weight(1f),
                    colors       = SliderDefaults.colors(
                        thumbColor        = CursorBlue,
                        activeTrackColor  = CursorBlue,
                        inactiveTrackColor = Color(0xFF444444)
                    )
                )
                Text(
                    "${fontSize.toInt()}",
                    color    = Color(0xFFE0E0E0),
                    fontSize = 13.sp,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("行距", color = Color(0xFFB3B3B3), fontSize = 13.sp, modifier = Modifier.width(36.dp))
                Slider(
                    value        = lineHeight,
                    onValueChange = onLineHeightChange,
                    valueRange   = 1.2f..3.0f,
                    steps        = 8,
                    modifier     = Modifier.weight(1f),
                    colors       = SliderDefaults.colors(
                        thumbColor        = CursorBlue,
                        activeTrackColor  = CursorBlue,
                        inactiveTrackColor = Color(0xFF444444)
                    )
                )
                Text(
                    String.format("%.1f", lineHeight),
                    color    = Color(0xFFE0E0E0),
                    fontSize = 13.sp,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
