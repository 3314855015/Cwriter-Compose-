package com.cwriter.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cwriter.data.model.Foreshadowing
import com.cwriter.data.model.ForeshadowingStatus

/**
 * 底部弹窗标签页
 */
enum class BottomSheetTab {
    CREATE,   // 创建伏笔
    RECYCLE   // 回收伏笔
}

/**
 * 伏笔底部弹窗组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForeshadowingBottomSheet(
    isVisible: Boolean,
    paragraphIndex: Int,
    currentChapterId: String,
    foreshadowings: List<Foreshadowing>,
    allChapterPairs: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    onCreateForeshadowing: (String) -> Unit,
    onRecycleForeshadowing: (String) -> Unit,
    onUnrecycleForeshadowing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var selectedTab by remember { mutableStateOf(BottomSheetTab.CREATE) }

    // 深色主题色
    val darkBackground = Color(0xFF1E1E1E)
    val darkSurface = Color(0xFF2D2D2D)
    val darkPrimary = Color(0xFFBB86FC)
    val darkOnSurface = Color(0xFFE1E1E1)

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier.fillMaxHeight(0.85f),
            containerColor = darkBackground,
            dragHandle = { 
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                ForeshadowingSheetHeader(
                    paragraphIndex = paragraphIndex,
                    onClose = onDismiss,
                    textColor = darkOnSurface
                )

                // 切换栏
                TabSwitcher(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    backgroundColor = darkSurface,
                    selectedColor = darkPrimary,
                    textColor = darkOnSurface
                )

                // 功能区
                when (selectedTab) {
                    BottomSheetTab.CREATE -> CreateForeshadowingTab(
                        paragraphIndex = paragraphIndex,
                        currentChapterId = currentChapterId,
                        foreshadowings = foreshadowings,
                        allChapterPairs = allChapterPairs,
                        onCreate = onCreateForeshadowing,
                        surfaceColor = darkSurface,
                        textColor = darkOnSurface,
                        accentColor = darkPrimary
                    )
                    BottomSheetTab.RECYCLE -> RecycleForeshadowingTab(
                        paragraphIndex = paragraphIndex,
                        currentChapterId = currentChapterId,
                        foreshadowings = foreshadowings,
                        allChapterPairs = allChapterPairs,
                        onRecycle = onRecycleForeshadowing,
                        onUnrecycle = onUnrecycleForeshadowing,
                        surfaceColor = darkSurface,
                        textColor = darkOnSurface,
                        accentColor = darkPrimary
                    )
                }
            }
        }
    }
}

/**
 * 弹窗头部
 */
@Composable
private fun ForeshadowingSheetHeader(
    paragraphIndex: Int,
    onClose: () -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "第 ${paragraphIndex + 1} 段",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 切换栏
 */
@Composable
private fun TabSwitcher(
    selectedTab: BottomSheetTab,
    onTabSelected: (BottomSheetTab) -> Unit,
    backgroundColor: Color,
    selectedColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(4.dp)
    ) {
        TabButton(
            text = "创建伏笔",
            isSelected = selectedTab == BottomSheetTab.CREATE,
            onClick = { onTabSelected(BottomSheetTab.CREATE) },
            selectedColor = selectedColor,
            textColor = textColor,
            modifier = Modifier.weight(1f)
        )
        TabButton(
            text = "回收伏笔",
            isSelected = selectedTab == BottomSheetTab.RECYCLE,
            onClick = { onTabSelected(BottomSheetTab.RECYCLE) },
            selectedColor = selectedColor,
            textColor = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 切换按钮
 */
@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) selectedColor else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * 创建伏笔标签页
 * 显示在当前章节当前段落创建的所有伏笔（包括已回收的）
 */
@Composable
private fun CreateForeshadowingTab(
    paragraphIndex: Int,
    currentChapterId: String,
    foreshadowings: List<Foreshadowing>,
    allChapterPairs: List<Pair<String, String>> = emptyList(),
    onCreate: (String) -> Unit,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color
) {
    var inputText by remember { mutableStateOf("") }
    
    // 筛选在当前章节当前段落创建的伏笔
    val createdHere = remember(foreshadowings, paragraphIndex, currentChapterId) {
        foreshadowings.filter {
            it.createdParagraphIndex == paragraphIndex && it.chapterId == currentChapterId
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 滚动区 - 显示已有伏笔
        if (createdHere.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(createdHere, key = { it.id }) { foreshadowing ->
                    ForeshadowingCard(
                        foreshadowing = foreshadowing,
                        isRecycled = foreshadowing.status == ForeshadowingStatus.RECYCLED,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        accentColor = accentColor,
                        allChapterPairs = allChapterPairs
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无伏笔",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }

        // 创建栏
        CreateInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            onSubmit = {
                if (inputText.isNotBlank()) {
                    onCreate(inputText)
                    inputText = ""
                }
            },
            surfaceColor = surfaceColor,
            textColor = textColor,
            accentColor = accentColor
        )
    }
}

/**
 * 伏笔卡片
 */
@Composable
private fun ForeshadowingCard(
    foreshadowing: Foreshadowing,
    isRecycled: Boolean = false,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color,
    onLongClick: (() -> Unit)? = null,
    allChapterPairs: List<Pair<String, String>> = emptyList()
) {
    // 获取章节序号（用于显示"第X章"）
    fun getChapterLabel(chapterId: String?): String {
        if (chapterId == null) return ""
        val idx = allChapterPairs.indexOfFirst { it.first == chapterId }
        return if (idx >= 0) "第${idx + 1}章" else ""
    }

    val cardModifier = if (onLongClick != null) {
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            }
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = foreshadowing.content,
                fontSize = 14.sp,
                color = if (isRecycled) textColor.copy(alpha = 0.6f) else textColor,
                lineHeight = 20.sp,
                textDecoration = if (isRecycled) TextDecoration.LineThrough else null
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = foreshadowing.getFormattedTime(),
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
                if (isRecycled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已回收",
                            fontSize = 10.sp,
                            color = accentColor
                        )
                        val recycledLabel = getChapterLabel(foreshadowing.recycledChapterId)
                        if (recycledLabel.isNotEmpty()) {
                            Text(
                                text = " · $recycledLabel",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                        if (foreshadowing.recycledParagraphIndex != null) {
                            Text(
                                text = " · 第${foreshadowing.recycledParagraphIndex + 1}段",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 创建输入栏
 */
@Composable
private fun CreateInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { 
                Text(
                    "写下新的伏笔...",
                    color = textColor.copy(alpha = 0.4f)
                ) 
            },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            maxLines = 3
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (value.isNotBlank()) accentColor 
                    else textColor.copy(alpha = 0.2f)
                )
                .clickable(enabled = value.isNotBlank(), onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "发布",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 回收伏笔标签页
 * 顶部：在当前段落回收的伏笔（可长按取消回收）
 * 下面：所有待回收的伏笔（跨章节）
 */
@Composable
private fun RecycleForeshadowingTab(
    paragraphIndex: Int,
    currentChapterId: String,
    foreshadowings: List<Foreshadowing>,
    allChapterPairs: List<Pair<String, String>> = emptyList(),
    onRecycle: (String) -> Unit,
    onUnrecycle: (String) -> Unit,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showUnrecycleDialog by remember { mutableStateOf<String?>(null) }
    
    // 在当前段落回收的伏笔
    val recycledHere = remember(foreshadowings, paragraphIndex, currentChapterId) {
        foreshadowings.filter { 
            it.status == ForeshadowingStatus.RECYCLED && 
            it.recycledChapterId == currentChapterId &&
            it.recycledParagraphIndex == paragraphIndex 
        }
    }
    
    // 所有待回收的伏笔（跨章节），排除同章节同段落创建的伏笔（不能自己回收自己）
    val pendingForeshadowings = remember(foreshadowings, currentChapterId, paragraphIndex) {
        foreshadowings.filter {
            it.status == ForeshadowingStatus.PENDING &&
            !(it.chapterId == currentChapterId && it.createdParagraphIndex == paragraphIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 已在本段回收的伏笔（顶部固定显示）
        if (recycledHere.isNotEmpty()) {
            Text(
                text = "本段已回收（长按可取消）",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recycledHere, key = { it.id }) { foreshadowing ->
                    ForeshadowingCard(
                        foreshadowing = foreshadowing,
                        isRecycled = true,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        accentColor = accentColor,
                        onLongClick = { showUnrecycleDialog = foreshadowing.id },
                        allChapterPairs = allChapterPairs
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 待回收的伏笔列表
        Text(
            text = "待回收伏笔（长按选择，可跨章节）",
            fontSize = 12.sp,
            color = textColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (pendingForeshadowings.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingForeshadowings, key = { it.id }) { foreshadowing ->
                    val isSelected = selectedIds.contains(foreshadowing.id)
                    SelectableForeshadowingCard(
                        foreshadowing = foreshadowing,
                        isSelected = isSelected,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        accentColor = accentColor,
                        allChapterPairs = allChapterPairs,
                        onLongClick = {
                            selectedIds = if (isSelected) {
                                selectedIds - foreshadowing.id
                            } else {
                                selectedIds + foreshadowing.id
                            }
                        }
                    )
                }
            }

            // 确认按钮
            if (selectedIds.isNotEmpty()) {
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确认回收 (${selectedIds.size})")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无待回收的伏笔",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    }

    // 确认回收对话框
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("回收伏笔") },
            text = { Text("确定要回收选中的 ${selectedIds.size} 个伏笔吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedIds.forEach { onRecycle(it) }
                        selectedIds = emptySet()
                        showConfirmDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 取消回收对话框
    showUnrecycleDialog?.let { foreshadowingId ->
        AlertDialog(
            onDismissRequest = { showUnrecycleDialog = null },
            title = { Text("取消回收") },
            text = { Text("确定要取消回收这个伏笔吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUnrecycle(foreshadowingId)
                        showUnrecycleDialog = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnrecycleDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 可选择的长按卡片
 */
@Composable
private fun SelectableForeshadowingCard(
    foreshadowing: Foreshadowing,
    isSelected: Boolean,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color,
    onLongClick: () -> Unit,
    allChapterPairs: List<Pair<String, String>> = emptyList()
) {
    // 获取章节序号
    fun getChapterLabel(chapterId: String): String {
        val idx = allChapterPairs.indexOfFirst { it.first == chapterId }
        return if (idx >= 0) "第${idx + 1}章" else ""
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                accentColor.copy(alpha = 0.2f)
            else 
                surfaceColor
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp, 
                accentColor
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = foreshadowing.content,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${getChapterLabel(foreshadowing.chapterId)} · 第 ${foreshadowing.createdParagraphIndex + 1} 段 · ${foreshadowing.getFormattedTime()}",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
