package com.cwriter.ui.components

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ─── 数据模型 ─────────────────────────────────────────────────────────────────

data class NestedItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val children: MutableList<NestedItem> = mutableListOf()
)

// 路径栈帧：记录每一层的父项列表 + 当前选中项
data class PathFrame(
    val items: List<NestedItem>,
    val selectedItem: NestedItem?
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

/**
 * NestedListViewModel
 *
 * 核心数据结构：
 *  rootItems        — 根级列表（持久化的顶层数据）
 *  currentLevelItems — 当前层级显示在父栏的列表
 *  selectedItem     — 父栏当前选中项（子栏显示其 children）
 *  childItems       — 子栏列表（= selectedItem.children）
 *  pathStack        — 路径栈，每次"进入子层级"时压栈，"返回"时弹栈
 *
 * 面试讲解：
 *  - 多级列表用路径栈实现无限层级导航，类似文件系统的 cd/cd..
 *  - 持久化用 SharedPreferences + JSON，key 按 workId 隔离
 *  - 所有 IO 操作在 viewModelScope + Dispatchers.IO，不阻塞主线程
 */
class NestedListViewModel : ViewModel() {

    private val _currentLevelItems = MutableStateFlow<List<NestedItem>>(emptyList())
    val currentLevelItems: StateFlow<List<NestedItem>> = _currentLevelItems.asStateFlow()

    private val _selectedItem = MutableStateFlow<NestedItem?>(null)
    val selectedItem: StateFlow<NestedItem?> = _selectedItem.asStateFlow()

    private val _childItems = MutableStateFlow<List<NestedItem>>(emptyList())
    val childItems: StateFlow<List<NestedItem>> = _childItems.asStateFlow()

    private val _isAtTopLevel = MutableStateFlow(true)
    val isAtTopLevel: StateFlow<Boolean> = _isAtTopLevel.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    var onDuplicateCallback: ((String) -> Unit)? = null

    private var rootItems: MutableList<NestedItem> = mutableListOf()
    // currentLevelItemsMutable 是 _currentLevelItems 的可变引用（同一对象）
    private var currentLevelMutable: MutableList<NestedItem> = mutableListOf()
    private val pathStack: MutableList<PathFrame> = mutableListOf()

    private var context: Context? = null
    private var workId: String = ""

    // ── 初始化 ──────────────────────────────────────────────────────────────

    fun init(context: Context, workId: String) {
        this.context = context
        this.workId = workId
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = loadFromPrefs()
            withContext(Dispatchers.Main) {
                rootItems = loaded
                currentLevelMutable = rootItems
                pathStack.clear()
                _currentLevelItems.value = currentLevelMutable.toList()
                _selectedItem.value = null
                _childItems.value = emptyList()
                _isAtTopLevel.value = true
                _isEditMode.value = false
            }
        }
    }

    // ── 导航 ─────────────────────────────────────────────────────────────────

    fun selectItem(item: NestedItem) {
        _selectedItem.value = item
        _childItems.value = item.children.toList()
    }

    /** 点击子项 → 进入子层级（子项列表变成新的父栏） */
    fun enterChildLevel(item: NestedItem) {
        val current = _selectedItem.value ?: return
        pathStack.add(PathFrame(currentLevelMutable.toList(), current))
        currentLevelMutable = current.children
        _currentLevelItems.value = currentLevelMutable.toList()
        _selectedItem.value = item
        _childItems.value = item.children.toList()
        _isAtTopLevel.value = false
    }

    /** 返回上一层 */
    fun goBack() {
        if (pathStack.isEmpty()) return
        val frame = pathStack.removeLast()
        // 恢复上一层的可变引用
        currentLevelMutable = if (pathStack.isEmpty()) rootItems
                              else pathStack.last().selectedItem!!.children
        _currentLevelItems.value = frame.items
        _selectedItem.value = frame.selectedItem
        _childItems.value = frame.selectedItem?.children?.toList() ?: emptyList()
        _isAtTopLevel.value = pathStack.isEmpty()
    }

    // ── 编辑模式 ─────────────────────────────────────────────────────────────

    fun toggleEditMode() { _isEditMode.value = !_isEditMode.value }

    // ── 新增 ─────────────────────────────────────────────────────────────────

    fun addParentItem(content: String): Boolean {
        if (currentLevelMutable.any { it.content == content }) {
            onDuplicateCallback?.invoke("「$content」已存在于当前列表中")
            return false
        }
        currentLevelMutable.add(NestedItem(content = content))
        _currentLevelItems.value = currentLevelMutable.toList()
        save(); return true
    }

    fun addChildItem(content: String): Boolean {
        val parent = _selectedItem.value ?: return false
        if (parent.children.any { it.content == content }) {
            onDuplicateCallback?.invoke("「$content」已存在于当前子列表中")
            return false
        }
        parent.children.add(NestedItem(content = content))
        _childItems.value = parent.children.toList()
        save(); return true
    }

    // ── 删除 ─────────────────────────────────────────────────────────────────

    fun deleteParentItem(item: NestedItem) {
        currentLevelMutable.removeAll { it.id == item.id }
        if (_selectedItem.value?.id == item.id) {
            _selectedItem.value = null
            _childItems.value = emptyList()
        }
        _currentLevelItems.value = currentLevelMutable.toList()
        save()
    }

    fun deleteChildItem(item: NestedItem) {
        val parent = _selectedItem.value ?: return
        parent.children.removeAll { it.id == item.id }
        _childItems.value = parent.children.toList()
        save()
    }

    // ── 拖拽排序 ─────────────────────────────────────────────────────────────

    fun reorderParent(from: Int, to: Int) {
        if (from == to) return
        if (from !in currentLevelMutable.indices || to !in currentLevelMutable.indices) return
        val item = currentLevelMutable.removeAt(from)
        currentLevelMutable.add(to, item)
        _currentLevelItems.value = currentLevelMutable.toList()
        save()
    }

    fun reorderChild(from: Int, to: Int) {
        val parent = _selectedItem.value ?: return
        if (from == to) return
        if (from !in parent.children.indices || to !in parent.children.indices) return
        val item = parent.children.removeAt(from)
        parent.children.add(to, item)
        _childItems.value = parent.children.toList()
        save()
    }

    // ── 持久化 ───────────────────────────────────────────────────────────────

    private fun save() {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val json = JSONObject().put("rootItems", itemListToJson(rootItems))
            ctx.getSharedPreferences("cwriter_nested", Context.MODE_PRIVATE)
                .edit().putString("nested_$workId", json.toString()).apply()
        }
    }

    private fun loadFromPrefs(): MutableList<NestedItem> {
        val ctx = context ?: return mutableListOf()
        val str = ctx.getSharedPreferences("cwriter_nested", Context.MODE_PRIVATE)
            .getString("nested_$workId", null) ?: return mutableListOf()
        return try {
            jsonToItemList(JSONObject(str).optJSONArray("rootItems"))
        } catch (e: Exception) { mutableListOf() }
    }

    private fun itemListToJson(list: List<NestedItem>): JSONArray = JSONArray().also { arr ->
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id",       item.id)
                put("content",  item.content)
                put("children", itemListToJson(item.children))
            })
        }
    }

    private fun jsonToItemList(arr: JSONArray?): MutableList<NestedItem> {
        arr ?: return mutableListOf()
        return (0 until arr.length()).mapTo(mutableListOf()) { i ->
            val obj = arr.getJSONObject(i)
            NestedItem(
                id       = obj.optString("id", java.util.UUID.randomUUID().toString()),
                content  = obj.optString("content"),
                children = jsonToItemList(obj.optJSONArray("children"))
            )
        }
    }
}

// ─── 主入口 Composable ────────────────────────────────────────────────────────

/**
 * NestedListPanel — 多级列表面板（功能E）
 *
 * 布局与 GlossaryPanel 完全对称：
 *  - 从右侧滑入，贴顶栏下方（statusBar + 56dp）
 *  - 宽2/3屏，高2/3屏
 *  - 左1/3：父栏（返回 + 新增 + 列表）
 *  - 右2/3：子栏（当前选中标题 + 新增子项 + 列表 + FAB）
 *
 * 与 GlossaryPanel 的区别：
 *  - 子项点击不是"插入文字"，而是"进入子层级"（弹确认框）
 *  - 父栏顶部有"返回"按钮，支持多级导航
 *  - 数据结构是无限嵌套树，而非两层固定结构
 */
@Composable
fun NestedListPanel(
    isVisible: Boolean,
    workId: String = "",
    onDismiss: () -> Unit,
    viewModel: NestedListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context       = LocalContext.current
    val configuration = LocalConfiguration.current
    val density       = LocalDensity.current

    val panelWidth  = configuration.screenWidthDp.dp * 2 / 3
    val panelHeight = configuration.screenHeightDp.dp * 2 / 3

    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val topOffset = statusBarHeight + 56.dp

    val currentLevelItems by viewModel.currentLevelItems.collectAsState()
    val selectedItem      by viewModel.selectedItem.collectAsState()
    val childItems        by viewModel.childItems.collectAsState()
    val isAtTopLevel      by viewModel.isAtTopLevel.collectAsState()
    val isEditMode        by viewModel.isEditMode.collectAsState()

    var showAddDialog    by remember { mutableStateOf(false) }
    var isAddingParent   by remember { mutableStateOf(true) }
    var duplicateMessage by remember { mutableStateOf<String?>(null) }

    var deleteParentTarget by remember { mutableStateOf<NestedItem?>(null) }
    var deleteChildTarget  by remember { mutableStateOf<NestedItem?>(null) }
    var confirmEnterItem   by remember { mutableStateOf<NestedItem?>(null) }
    var detailItem         by remember { mutableStateOf<NestedItem?>(null) }

    LaunchedEffect(workId) {
        viewModel.init(context, workId)
        viewModel.onDuplicateCallback = { msg -> duplicateMessage = msg }
    }

    // 滑入动画（与 GlossaryPanel 完全相同）
    val transition = updateTransition(isVisible, label = "nested_panel")
    val offsetX by transition.animateDp(
        label = "offsetX",
        transitionSpec = {
            if (targetState) tween(300, easing = FastOutSlowInEasing)
            else             tween(250, easing = FastOutLinearInEasing)
        }
    ) { visible -> if (visible) 0.dp else panelWidth }
    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = { if (targetState) tween(300) else tween(250) }
    ) { visible -> if (visible) 1f else 0f }

    if (!isVisible && offsetX >= panelWidth) return

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Surface(
            modifier = Modifier
                .padding(top = topOffset)
                .width(panelWidth)
                .height(panelHeight)
                .offset(x = offsetX)
                .alpha(alpha),
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(bottomStart = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {

                // ── 左栏：父项（1/3）────────────────────────────────────
                NestedParentColumn(
                    modifier         = Modifier.weight(1f),
                    items            = currentLevelItems,
                    selectedItem     = selectedItem,
                    isAtTopLevel     = isAtTopLevel,
                    isEditMode       = isEditMode,
                    onBackClick      = { if (isAtTopLevel) onDismiss() else viewModel.goBack() },
                    onAddClick       = { isAddingParent = true; duplicateMessage = null; showAddDialog = true },
                    onItemClick      = { item -> if (!isEditMode) viewModel.selectItem(item) },
                    onItemLongPress  = { item -> if (!isEditMode) detailItem = item
                                                 else deleteParentTarget = item },
                    onReorder        = { from, to -> viewModel.reorderParent(from, to) }
                )

                // 竖分隔线
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF333333)))

                // ── 右栏：子项（2/3）+ FAB ───────────────────────────────
                NestedChildColumn(
                    modifier        = Modifier.weight(2f),
                    selectedItem    = selectedItem,
                    items           = childItems,
                    isEditMode      = isEditMode,
                    onAddClick      = {
                        if (selectedItem != null) {
                            isAddingParent = false; duplicateMessage = null; showAddDialog = true
                        }
                    },
                    onItemClick     = { item ->
                        if (isEditMode) deleteChildTarget = item
                        else confirmEnterItem = item
                    },
                    onItemLongPress = { item -> if (!isEditMode) detailItem = item },
                    onToggleEditMode = { viewModel.toggleEditMode() },
                    onReorder       = { from, to -> viewModel.reorderChild(from, to) }
                )
            }
        }
    }

    // ── 新增对话框 ────────────────────────────────────────────────────────────
    if (showAddDialog) {
        NestedAddDialog(
            title            = if (isAddingParent) "新增" else "新增子项",
            duplicateMessage = duplicateMessage,
            onDismiss        = { showAddDialog = false; duplicateMessage = null },
            onConfirm        = { content ->
                val ok = if (isAddingParent) viewModel.addParentItem(content)
                         else viewModel.addChildItem(content)
                if (ok) { showAddDialog = false; duplicateMessage = null }
            }
        )
    }

    // ── 进入子层级确认 ────────────────────────────────────────────────────────
    confirmEnterItem?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmEnterItem = null },
            title   = { Text("进入子层级") },
            text    = { Text("确定要将「${item.content}」作为新的父级显示吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.enterChildLevel(item); confirmEnterItem = null }) {
                    Text("确定", color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnterItem = null }) { Text("取消") }
            },
            containerColor = Color(0xFF252525),
            titleContentColor = Color(0xFFE0E0E0),
            textContentColor = Color(0xFFB3B3B3)
        )
    }

    // ── 删除父项确认 ──────────────────────────────────────────────────────────
    deleteParentTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteParentTarget = null },
            title   = { Text("删除") },
            text    = { Text("确定要删除「${target.content}」及其全部子项吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteParentItem(target); deleteParentTarget = null }) {
                    Text("删除", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteParentTarget = null }) { Text("取消") }
            },
            containerColor = Color(0xFF252525),
            titleContentColor = Color(0xFFE0E0E0),
            textContentColor = Color(0xFFB3B3B3)
        )
    }

    // ── 删除子项确认 ──────────────────────────────────────────────────────────
    deleteChildTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteChildTarget = null },
            title   = { Text("删除子项") },
            text    = { Text("确定要删除「${target.content}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteChildItem(target); deleteChildTarget = null }) {
                    Text("删除", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteChildTarget = null }) { Text("取消") }
            },
            containerColor = Color(0xFF252525),
            titleContentColor = Color(0xFFE0E0E0),
            textContentColor = Color(0xFFB3B3B3)
        )
    }

    // ── 长按详情 ──────────────────────────────────────────────────────────────
    detailItem?.let { item ->
        Dialog(
            onDismissRequest = { detailItem = null },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF252525)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(item.content, color = Color(0xFFE0E0E0), fontSize = 20.sp)
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { detailItem = null }) { Text("关闭", color = Color(0xFF007AFF)) }
                    }
                }
            }
        }
    }
}

// ─── 子 Composable ────────────────────────────────────────────────────────────

@Composable
private fun NestedParentColumn(
    modifier: Modifier,
    items: List<NestedItem>,
    selectedItem: NestedItem?,
    isAtTopLevel: Boolean,
    isEditMode: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (NestedItem) -> Unit,
    onItemLongPress: (NestedItem) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetY   by remember { mutableStateOf(0f) }
    val itemHeightPx  = with(LocalDensity.current) { 48.dp.toPx() }

    Column(modifier = modifier.fillMaxHeight()) {
        // 返回按钮
        Row(
            modifier = Modifier
                .fillMaxWidth().height(48.dp)
                .background(Color(0xFF252525))
                .pointerInput(Unit) { detectTapGestures(onTap = { onBackClick() }) }
                .padding(horizontal = 12.dp)
                .alpha(if (isAtTopLevel) 0.4f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFFE0E0E0), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (isAtTopLevel) "关闭" else "返回", color = Color(0xFFE0E0E0), fontSize = 13.sp)
        }
        // 新增按钮
        Row(
            modifier = Modifier
                .fillMaxWidth().height(48.dp)
                .background(Color(0xFF252525))
                .pointerInput(Unit) { detectTapGestures(onTap = { onAddClick() }) }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, "新增", tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("新增", color = Color(0xFF007AFF), fontSize = 13.sp)
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF333333))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                NestedDraggableRow(
                    content     = item.content,
                    isSelected  = item.id == selectedItem?.id && !isEditMode,
                    isDragging  = draggingIndex == index,
                    dragOffsetY = if (draggingIndex == index) dragOffsetY else 0f,
                    isEditMode  = isEditMode,
                    onClick     = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    onDragStart = if (isEditMode) { off -> draggingIndex = index; dragOffsetY = off } else null,
                    onDragMove  = if (isEditMode) { off -> dragOffsetY = off } else null,
                    onDragEnd   = if (isEditMode) { {
                        if (draggingIndex >= 0) {
                            val steps = (dragOffsetY / itemHeightPx).toInt()
                            val target = (draggingIndex + steps).coerceIn(0, items.size - 1)
                            if (target != draggingIndex) onReorder(draggingIndex, target)
                        }
                        draggingIndex = -1; dragOffsetY = 0f
                    } } else null,
                    childCount  = item.children.size
                )
            }
            if (items.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("点击上方新增", color = Color(0xFF666666), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NestedChildColumn(
    modifier: Modifier,
    selectedItem: NestedItem?,
    items: List<NestedItem>,
    isEditMode: Boolean,
    onAddClick: () -> Unit,
    onItemClick: (NestedItem) -> Unit,
    onItemLongPress: (NestedItem) -> Unit,
    onToggleEditMode: () -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetY   by remember { mutableStateOf(0f) }
    val itemHeightPx  = with(LocalDensity.current) { 48.dp.toPx() }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 当前选中项标题
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(48.dp)
                    .background(Color(0xFF252525))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text     = selectedItem?.content ?: "请选择父项",
                    color    = if (selectedItem != null) Color(0xFFE0E0E0) else Color(0xFF666666),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 新增子项
            Row(
                modifier = Modifier
                    .fillMaxWidth().height(48.dp)
                    .background(Color(0xFF252525))
                    .pointerInput(selectedItem) {
                        if (selectedItem != null) detectTapGestures(onTap = { onAddClick() })
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, "新增子项",
                    tint = if (selectedItem != null) Color(0xFF007AFF) else Color(0xFF444444),
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("新增子项",
                    color = if (selectedItem != null) Color(0xFF007AFF) else Color(0xFF444444),
                    fontSize = 13.sp)
            }
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF333333))

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                    NestedDraggableRow(
                        content     = item.content,
                        isSelected  = false,
                        isDragging  = draggingIndex == index,
                        dragOffsetY = if (draggingIndex == index) dragOffsetY else 0f,
                        isEditMode  = isEditMode,
                        onClick     = { onItemClick(item) },
                        onLongPress = { onItemLongPress(item) },
                        onDragStart = if (isEditMode) { off -> draggingIndex = index; dragOffsetY = off } else null,
                        onDragMove  = if (isEditMode) { off -> dragOffsetY = off } else null,
                        onDragEnd   = if (isEditMode) { {
                            if (draggingIndex >= 0) {
                                val steps = (dragOffsetY / itemHeightPx).toInt()
                                val target = (draggingIndex + steps).coerceIn(0, items.size - 1)
                                if (target != draggingIndex) onReorder(draggingIndex, target)
                            }
                            draggingIndex = -1; dragOffsetY = 0f
                        } } else null,
                        childCount  = item.children.size
                    )
                }
                if (items.isEmpty() && selectedItem != null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("暂无子项，点击上方新增", color = Color(0xFF666666), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // FAB：查看模式 Edit，编辑模式 Check
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd).padding(12.dp)
                .size(40.dp).clip(CircleShape)
                .background(Color(0xFF007AFF))
                .pointerInput(Unit) { detectTapGestures(onTap = { onToggleEditMode() }) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = if (isEditMode) "完成" else "编辑",
                tint               = Color.White,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 通用可拖拽行（父栏和子栏共用）
 * 手势：< 500ms 短按，≥ 500ms 长按/拖拽
 */
@Composable
private fun NestedDraggableRow(
    content: String,
    isSelected: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: ((Float) -> Unit)?,
    onDragMove: ((Float) -> Unit)?,
    onDragEnd: (() -> Unit)?,
    childCount: Int = 0
) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = with(density) { dragOffsetY.toDp() })
            .background(
                when {
                    isDragging -> Color(0xFF007AFF).copy(alpha = 0.15f)
                    isSelected -> Color(0xFF007AFF).copy(alpha = 0.2f)
                    else       -> Color.Transparent
                }
            )
            .pointerInput(isEditMode) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        var longPressed = false
                        var totalOffsetY = 0f
                        val startTime = System.currentTimeMillis()
                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            val elapsed = System.currentTimeMillis() - startTime
                            val change = event.changes.firstOrNull() ?: break
                            if (!longPressed && elapsed >= 500L) {
                                longPressed = true
                                if (isEditMode && onDragStart != null) onDragStart(totalOffsetY)
                                else onLongPress()
                            }
                            if (longPressed && isEditMode && onDragMove != null) {
                                totalOffsetY += change.positionChange().y
                                onDragMove(totalOffsetY)
                                change.consume()
                            }
                            event = awaitPointerEvent()
                        }
                        if (longPressed) onDragEnd?.invoke()
                        else if (System.currentTimeMillis() - startTime < 500L) onClick()
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = content,
            color    = if (isSelected) Color(0xFF007AFF) else Color(0xFFE0E0E0),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // 子项数量标记（有子项时显示）
        if (childCount > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF007AFF).copy(alpha = 0.2f),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text     = "$childCount",
                    color    = Color(0xFF007AFF),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ─── 新增对话框 ───────────────────────────────────────────────────────────────

@Composable
private fun NestedAddDialog(
    title: String,
    duplicateMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF252525)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, color = Color(0xFFE0E0E0), fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("请输入内容", color = Color(0xFF666666)) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE0E0E0), fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = Color(0xFF007AFF)
                    ),
                    singleLine = true
                )
                if (duplicateMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF8B0000).copy(alpha = 0.8f)) {
                        Text(duplicateMessage, color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF888888)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(text.trim()) }) { Text("确定", color = Color(0xFF007AFF)) }
                }
            }
        }
    }
}
