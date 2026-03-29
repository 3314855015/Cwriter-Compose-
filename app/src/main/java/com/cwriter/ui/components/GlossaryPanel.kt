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
import androidx.compose.ui.text.font.FontWeight
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

enum class GlossaryType(val label: String) {
    CHARACTER("人物"),
    LOCATION("地点"),
    ABILITY("能力"),
    OTHER("其他")
}

data class GlossaryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val children: MutableList<GlossaryItem> = mutableListOf()
)

// 整个词库数据，按类型分桶
data class GlossaryData(
    val character: MutableList<GlossaryItem> = mutableListOf(),
    val location: MutableList<GlossaryItem> = mutableListOf(),
    val ability: MutableList<GlossaryItem> = mutableListOf(),
    val other: MutableList<GlossaryItem> = mutableListOf(),
    // 每个类型下"通用"这个特殊父项的子项（独立存储）
    val characterGeneral: MutableList<GlossaryItem> = mutableListOf(),
    val locationGeneral: MutableList<GlossaryItem> = mutableListOf(),
    val abilityGeneral: MutableList<GlossaryItem> = mutableListOf(),
    val otherGeneral: MutableList<GlossaryItem> = mutableListOf()
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

/**
 * GlossaryViewModel
 *
 * 职责：
 *  1. 持有词库数据（StateFlow 驱动 UI）
 *  2. 持久化到 SharedPreferences（key = "glossary_<workId>"）
 *  3. 暴露增删、类型切换、父/子项选择等操作
 */
class GlossaryViewModel : ViewModel() {

    // 当前选中类型
    private val _currentType = MutableStateFlow(GlossaryType.CHARACTER)
    val currentType: StateFlow<GlossaryType> = _currentType.asStateFlow()

    // null = 选中"通用"这个特殊父项
    private val _selectedItem = MutableStateFlow<GlossaryItem?>(null)
    val selectedItem: StateFlow<GlossaryItem?> = _selectedItem.asStateFlow()

    // 父栏列表（当前类型下用户创建的父项）
    private val _parentItems = MutableStateFlow<List<GlossaryItem>>(emptyList())
    val parentItems: StateFlow<List<GlossaryItem>> = _parentItems.asStateFlow()

    // 子栏列表（随 selectedItem 变化）
    private val _childItems = MutableStateFlow<List<GlossaryItem>>(emptyList())
    val childItems: StateFlow<List<GlossaryItem>> = _childItems.asStateFlow()

    // 编辑模式（编辑模式下点击项目弹删除确认）
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // 重复提示回调（传给 UI 显示）
    var onDuplicateCallback: ((String) -> Unit)? = null

    private var glossaryData = GlossaryData()
    private var context: Context? = null
    private var workId: String = ""

    // ── 初始化 ──────────────────────────────────────────────────────────────

    fun init(context: Context, workId: String) {
        this.context = context
        this.workId = workId
        viewModelScope.launch(Dispatchers.IO) {
            glossaryData = loadFromPrefs()
            withContext(Dispatchers.Main) {
                _selectedItem.value = null
                _isEditMode.value = false
                refreshLists()
            }
        }
    }

    // ── 类型切换 ─────────────────────────────────────────────────────────────

    fun switchType(type: GlossaryType) {
        _currentType.value = type
        _selectedItem.value = null   // 切换类型时默认回到"通用"
        refreshLists()
    }

    // ── 父项选择 ─────────────────────────────────────────────────────────────

    fun selectItem(item: GlossaryItem?) {
        _selectedItem.value = item
        _childItems.value = (item?.children ?: currentGeneralChildren()).toList()
    }

    // ── 编辑模式切换 ─────────────────────────────────────────────────────────

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    // ── 新增 ─────────────────────────────────────────────────────────────────

    /** 新增父项，返回 false 表示重名 */
    fun addParentItem(name: String): Boolean {
        if (currentParentList().any { it.name == name }) {
            onDuplicateCallback?.invoke("「$name」已存在于当前列表中")
            return false
        }
        currentParentList().add(GlossaryItem(name = name))
        save(); refreshLists()
        return true
    }

    /** 新增子项，返回 false 表示重名 */
    fun addChildItem(name: String): Boolean {
        val list = _selectedItem.value?.children ?: currentGeneralChildren()
        if (list.any { it.name == name }) {
            onDuplicateCallback?.invoke("「$name」已存在于当前子列表中")
            return false
        }
        list.add(GlossaryItem(name = name))
        save()
        _childItems.value = list.toList()
        return true
    }

    // ── 删除 ─────────────────────────────────────────────────────────────────

    /** 删除父项（含其所有子项） */
    fun deleteParentItem(item: GlossaryItem) {
        currentParentList().removeAll { it.id == item.id }
        // 如果删的是当前选中项，回到通用
        if (_selectedItem.value?.id == item.id) _selectedItem.value = null
        save(); refreshLists()
    }

    /** 删除子项 */
    fun deleteChildItem(item: GlossaryItem) {
        val list = _selectedItem.value?.children ?: currentGeneralChildren()
        list.removeAll { it.id == item.id }
        save()
        _childItems.value = list.toList()
    }

    // ── 拖拽排序 ─────────────────────────────────────────────────────────────

    /** 父项拖拽排序：把 fromIndex 的项移到 toIndex */
    fun reorderParent(fromIndex: Int, toIndex: Int) {
        val list = currentParentList()
        if (fromIndex == toIndex) return
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        save(); refreshLists()
    }

    /** 子项拖拽排序 */
    fun reorderChild(fromIndex: Int, toIndex: Int) {
        val list = _selectedItem.value?.children ?: currentGeneralChildren()
        if (fromIndex == toIndex) return
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        save()
        _childItems.value = list.toList()
    }

    // ── 内部工具 ─────────────────────────────────────────────────────────────

    private fun currentParentList(): MutableList<GlossaryItem> = when (_currentType.value) {
        GlossaryType.CHARACTER -> glossaryData.character
        GlossaryType.LOCATION  -> glossaryData.location
        GlossaryType.ABILITY   -> glossaryData.ability
        GlossaryType.OTHER     -> glossaryData.other
    }

    private fun currentGeneralChildren(): MutableList<GlossaryItem> = when (_currentType.value) {
        GlossaryType.CHARACTER -> glossaryData.characterGeneral
        GlossaryType.LOCATION  -> glossaryData.locationGeneral
        GlossaryType.ABILITY   -> glossaryData.abilityGeneral
        GlossaryType.OTHER     -> glossaryData.otherGeneral
    }

    private fun refreshLists() {
        _parentItems.value = currentParentList().toList()
        _childItems.value  = (_selectedItem.value?.children ?: currentGeneralChildren()).toList()
    }

    // ── 持久化 ───────────────────────────────────────────────────────────────

    /**
     * 持久化到 SharedPreferences
     * key = "glossary_<workId>"，value = JSON 字符串
     * 对应 UniApp 的 uni.setStorageSync(`glossary_data_${workId}`, data)
     */
    private fun save() {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val json = glossaryDataToJson(glossaryData)
            ctx.getSharedPreferences("cwriter_glossary", Context.MODE_PRIVATE)
                .edit().putString("glossary_$workId", json.toString()).apply()
        }
    }

    private fun loadFromPrefs(): GlossaryData {
        val ctx = context ?: return GlossaryData()
        val str = ctx.getSharedPreferences("cwriter_glossary", Context.MODE_PRIVATE)
            .getString("glossary_$workId", null) ?: return GlossaryData()
        return try { jsonToGlossaryData(JSONObject(str)) } catch (e: Exception) { GlossaryData() }
    }

    // ── JSON 序列化 ───────────────────────────────────────────────────────────

    private fun glossaryDataToJson(data: GlossaryData): JSONObject = JSONObject().apply {
        put("character",        itemListToJson(data.character))
        put("location",         itemListToJson(data.location))
        put("ability",          itemListToJson(data.ability))
        put("other",            itemListToJson(data.other))
        put("characterGeneral", itemListToJson(data.characterGeneral))
        put("locationGeneral",  itemListToJson(data.locationGeneral))
        put("abilityGeneral",   itemListToJson(data.abilityGeneral))
        put("otherGeneral",     itemListToJson(data.otherGeneral))
    }

    private fun itemListToJson(list: List<GlossaryItem>): JSONArray = JSONArray().also { arr ->
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id",       item.id)
                put("name",     item.name)
                put("children", itemListToJson(item.children))
            })
        }
    }

    private fun jsonToGlossaryData(json: JSONObject) = GlossaryData(
        character        = jsonToItemList(json.optJSONArray("character")),
        location         = jsonToItemList(json.optJSONArray("location")),
        ability          = jsonToItemList(json.optJSONArray("ability")),
        other            = jsonToItemList(json.optJSONArray("other")),
        characterGeneral = jsonToItemList(json.optJSONArray("characterGeneral")),
        locationGeneral  = jsonToItemList(json.optJSONArray("locationGeneral")),
        abilityGeneral   = jsonToItemList(json.optJSONArray("abilityGeneral")),
        otherGeneral     = jsonToItemList(json.optJSONArray("otherGeneral"))
    )

    private fun jsonToItemList(arr: JSONArray?): MutableList<GlossaryItem> {
        arr ?: return mutableListOf()
        return (0 until arr.length()).mapTo(mutableListOf()) { i ->
            val obj = arr.getJSONObject(i)
            GlossaryItem(
                id       = obj.optString("id", java.util.UUID.randomUUID().toString()),
                name     = obj.optString("name"),
                children = jsonToItemList(obj.optJSONArray("children"))
            )
        }
    }
}

// ─── 主入口 Composable ────────────────────────────────────────────────────────

/**
 * GlossaryPanel — 词库面板滑出框
 *
 * 布局讲解：
 *  - 整体用 Box(fillMaxSize, TopEnd) 作为锚点，面板贴右上角
 *  - Surface 用 offset(x = offsetX) 做从右侧滑入动画
 *    offsetX 由 updateTransition + animateDp 驱动：
 *    isVisible=true → 0.dp，isVisible=false → panelWidth（完全移出屏幕右侧）
 *  - 面板从顶栏下方开始（padding(top = statusBar + 56dp)），不遮挡顶栏
 *  - 内部 Column：TypeSwitcher + Divider + Row(左1/3父栏 | 竖线 | 右2/3子栏)
 *
 * 参数：
 *  @param workId 用于隔离不同作品的词库数据
 *  @param onInsertText 子项点击时回调，传入 item.name，由外层在光标位置插入
 */
@Composable
fun GlossaryPanel(
    isVisible: Boolean,
    workId: String = "",
    onDismiss: () -> Unit,
    onInsertText: (String) -> Unit = {},
    viewModel: GlossaryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context       = LocalContext.current
    val configuration = LocalConfiguration.current
    val density       = LocalDensity.current

    val panelWidth  = configuration.screenWidthDp.dp * 2 / 3
    val panelHeight = configuration.screenHeightDp.dp * 2 / 3

    // 顶栏总高度（状态栏 + 导航栏56dp）
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val topOffset = statusBarHeight + 56.dp

    val currentType  by viewModel.currentType.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val parentItems  by viewModel.parentItems.collectAsState()
    val childItems   by viewModel.childItems.collectAsState()
    val isEditMode   by viewModel.isEditMode.collectAsState()

    // 新增对话框状态
    var showAddDialog    by remember { mutableStateOf(false) }
    var isAddingParent   by remember { mutableStateOf(true) }
    var duplicateMessage by remember { mutableStateOf<String?>(null) }

    // 删除确认对话框
    var deleteParentTarget by remember { mutableStateOf<GlossaryItem?>(null) }
    var deleteChildTarget  by remember { mutableStateOf<GlossaryItem?>(null) }

    // 长按详情对话框（查看模式下长按子项）
    var detailItem by remember { mutableStateOf<GlossaryItem?>(null) }

    // 初始化（每次 workId 变化时重新加载）
    LaunchedEffect(workId) {
        viewModel.init(context, workId)
        viewModel.onDuplicateCallback = { msg -> duplicateMessage = msg }
    }

    // ── 滑入/滑出动画 ────────────────────────────────────────────────────────
    // updateTransition 比 AnimatedVisibility 更灵活：
    // 可以同时驱动多个属性（offsetX + alpha），且能精确控制 easing
    val transition = updateTransition(isVisible, label = "glossary_panel")

    val offsetX by transition.animateDp(
        label = "offsetX",
        transitionSpec = {
            if (targetState) tween(300, easing = FastOutSlowInEasing)
            else             tween(250, easing = FastOutLinearInEasing)
        }
    ) { visible -> if (visible) 0.dp else panelWidth }

    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = {
            if (targetState) tween(300) else tween(250)
        }
    ) { visible -> if (visible) 1f else 0f }

    // 面板完全移出时不渲染（节省 measure/draw）
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
            Column(modifier = Modifier.fillMaxSize()) {

                // ── 类型切换栏 ────────────────────────────────────────────
                GlossaryTypeSwitcher(
                    currentType  = currentType,
                    onTypeChange = { viewModel.switchType(it) }
                )

                HorizontalDivider(thickness = 1.dp, color = Color(0xFF333333))

                // ── 双栏区域 ─────────────────────────────────────────────
                Row(modifier = Modifier.weight(1f)) {

                    // 左栏：父项（1/3宽）
                    GlossaryParentColumn(
                        modifier       = Modifier.weight(1f),
                        selectedItem   = selectedItem,
                        items          = parentItems,
                        isEditMode     = isEditMode,
                        onAddClick     = {
                            isAddingParent = true
                            duplicateMessage = null
                            showAddDialog = true
                        },
                        onItemClick    = { item ->
                            if (isEditMode) deleteParentTarget = item
                            else viewModel.selectItem(item)
                        },
                        onItemLongPress = { item ->
                            // 查看模式：弹详情；编辑模式：拖拽（由行内处理，此处不触发）
                            if (!isEditMode) detailItem = item
                        },
                        onGeneralClick = {
                            if (!isEditMode) viewModel.selectItem(null)
                        },
                        onReorder = { from, to -> viewModel.reorderParent(from, to) }
                    )

                    // 竖分隔线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF333333))
                    )

                    // 右栏：子项（2/3宽）+ 编辑模式FAB
                    GlossaryChildColumn(
                        modifier    = Modifier.weight(2f),
                        items       = childItems,
                        isEditMode  = isEditMode,
                        onAddClick  = {
                            isAddingParent = false
                            duplicateMessage = null
                            showAddDialog = true
                        },
                        onItemClick = { item ->
                            if (isEditMode) deleteChildTarget = item
                            else onInsertText(item.name)
                        },
                        onItemLongPress = { item ->
                            if (!isEditMode) detailItem = item
                        },
                        onToggleEditMode = { viewModel.toggleEditMode() },
                        onReorder = { from, to -> viewModel.reorderChild(from, to) }
                    )
                }
            }
        }
    }

    // ── 新增对话框 ────────────────────────────────────────────────────────────
    if (showAddDialog) {
        GlossaryAddDialog(
            title            = if (isAddingParent) "新增" else "新增子项",
            duplicateMessage = duplicateMessage,
            onDismiss        = { showAddDialog = false; duplicateMessage = null },
            onConfirm        = { name ->
                val ok = if (isAddingParent) viewModel.addParentItem(name)
                         else viewModel.addChildItem(name)
                if (ok) { showAddDialog = false; duplicateMessage = null }
            }
        )
    }

    // ── 删除父项确认 ──────────────────────────────────────────────────────────
    deleteParentTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteParentTarget = null },
            title   = { Text("删除父项") },
            text    = { Text("确定要删除「${target.name}」及其全部子项吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteParentItem(target); deleteParentTarget = null }) {
                    Text("删除", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteParentTarget = null }) { Text("取消") }
            }
        )
    }

    // ── 删除子项确认 ──────────────────────────────────────────────────────────
    deleteChildTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteChildTarget = null },
            title   = { Text("删除子项") },
            text    = { Text("确定要删除「${target.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteChildItem(target); deleteChildTarget = null }) {
                    Text("删除", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteChildTarget = null }) { Text("取消") }
            }
        )
    }

    // ── 长按详情 ──────────────────────────────────────────────────────────────
    detailItem?.let { item ->
        GlossaryDetailDialog(item = item, onDismiss = { detailItem = null })
    }
}

// ─── 子 Composable ────────────────────────────────────────────────────────────

/**
 * 类型切换栏
 * 选中项蓝色加粗，未选中白色
 */
@Composable
private fun GlossaryTypeSwitcher(
    currentType: GlossaryType,
    onTypeChange: (GlossaryType) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF252525)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlossaryType.entries.forEach { type ->
                val selected = type == currentType
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(type) { detectTapGestures(onTap = { onTypeChange(type) }) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = type.label,
                        color      = if (selected) Color(0xFF007AFF) else Color(0xFFE0E0E0),
                        fontSize   = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 父项列（左1/3）
 *
 * 编辑模式行为：
 *  - 短按 → 弹删除确认（通用不可删）
 *  - 长按500ms → 拖拽排序（通用不可拖）
 * 查看模式行为：
 *  - 短按 → 切换选中
 *  - 长按 → 弹详情（通用不弹）
 */
@Composable
private fun GlossaryParentColumn(
    modifier: Modifier,
    selectedItem: GlossaryItem?,
    items: List<GlossaryItem>,
    isEditMode: Boolean,
    onAddClick: () -> Unit,
    onItemClick: (GlossaryItem) -> Unit,
    onItemLongPress: (GlossaryItem) -> Unit,
    onGeneralClick: () -> Unit,
    onReorder: (from: Int, to: Int) -> Unit
) {
    // 拖拽状态：draggingIndex = 正在拖的项在 items 中的索引，dragOffsetY = 拖拽偏移px
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetY   by remember { mutableStateOf(0f) }
    val itemHeightPx  = with(LocalDensity.current) { 48.dp.toPx() }

    Column(modifier = modifier.fillMaxHeight()) {
        // 新增栏
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
            // "通用"恒有项 — 不支持拖拽，查看模式不弹详情
            item {
                GlossaryParentRow(
                    name        = "通用",
                    isSelected  = selectedItem == null,
                    isEditMode  = isEditMode,
                    isDragging  = false,
                    dragOffsetY = 0f,
                    onClick     = { onGeneralClick() },
                    onLongPress = { /* 通用不弹详情，不拖拽 */ }
                )
            }
            // 用户创建的父项，支持拖拽（编辑模式）和长按详情（查看模式）
            itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                GlossaryParentRow(
                    name        = item.name,
                    isSelected  = item.id == selectedItem?.id,
                    isEditMode  = isEditMode,
                    isDragging  = draggingIndex == index,
                    dragOffsetY = if (draggingIndex == index) dragOffsetY else 0f,
                    onClick     = { onItemClick(item) },
                    onLongPress = {
                        if (isEditMode) {
                            // 编辑模式长按 → 开始拖拽（由 pointerInput 内部处理，此处仅标记）
                        } else {
                            onItemLongPress(item)
                        }
                    },
                    onDragStart = if (isEditMode) { offsetY ->
                        draggingIndex = index
                        dragOffsetY = offsetY
                    } else null,
                    onDragMove = if (isEditMode) { offsetY ->
                        dragOffsetY = offsetY
                    } else null,
                    onDragEnd = if (isEditMode) {
                        {
                            if (draggingIndex >= 0) {
                                val steps = (dragOffsetY / itemHeightPx).toInt()
                                val target = (draggingIndex + steps).coerceIn(0, items.size - 1)
                                if (target != draggingIndex) onReorder(draggingIndex, target)
                            }
                            draggingIndex = -1
                            dragOffsetY = 0f
                        }
                    } else null
                )
            }
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("点击上方新增", color = Color(0xFF666666), fontSize = 11.sp) }
                }
            }
        }
    }
}

/**
 * 父项行
 *
 * 手势逻辑（用 awaitPointerEventScope 精确控制）：
 *  - 按下后等待：< 500ms 抬起 → 短按（onClick）
 *  - 按下后等待：≥ 500ms 未抬起 → 长按（onLongPress 或开始拖拽）
 *  - 拖拽中移动 → onDragMove(累计偏移Y)
 *  - 拖拽中抬起 → onDragEnd()
 *
 * 为什么用 awaitPointerEventScope 而不是 detectTapGestures？
 *  detectTapGestures 的 onLongPress 触发后无法继续追踪移动，
 *  awaitPointerEventScope 可以在长按后继续消费 MOVE 事件实现拖拽。
 */
@Composable
private fun GlossaryParentRow(
    name: String,
    isSelected: Boolean,
    isEditMode: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: ((Float) -> Unit)? = null,
    onDragMove: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = with(density) { dragOffsetY.toDp() })
            .background(
                when {
                    isDragging -> Color(0xFF007AFF).copy(alpha = 0.15f)
                    isSelected && !isEditMode -> Color(0xFF007AFF).copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .pointerInput(isEditMode) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var longPressed = false
                        var totalOffsetY = 0f
                        val startTime = System.currentTimeMillis()

                        // 等待 500ms 或手指抬起
                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            val elapsed = System.currentTimeMillis() - startTime
                            val change = event.changes.firstOrNull() ?: break

                            if (!longPressed && elapsed >= 500L) {
                                longPressed = true
                                if (onDragStart != null) {
                                    onDragStart(totalOffsetY)
                                } else {
                                    onLongPress()
                                }
                            }

                            if (longPressed && onDragMove != null) {
                                totalOffsetY += change.positionChange().y
                                onDragMove(totalOffsetY)
                                change.consume()
                            }

                            event = awaitPointerEvent()
                        }

                        if (longPressed) {
                            onDragEnd?.invoke()
                        } else {
                            // 短按
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed < 500L) onClick()
                        }
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text     = name,
            color    = when {
                isSelected && !isEditMode -> Color(0xFF007AFF)
                else -> Color(0xFFE0E0E0)
            },
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 子项列（右2/3）
 *
 * FAB 图标：查看模式 = Edit 图标，编辑模式 = Check 图标（表示"完成编辑"）
 */
@Composable
private fun GlossaryChildColumn(
    modifier: Modifier,
    items: List<GlossaryItem>,
    isEditMode: Boolean,
    onAddClick: () -> Unit,
    onItemClick: (GlossaryItem) -> Unit,
    onItemLongPress: (GlossaryItem) -> Unit,
    onToggleEditMode: () -> Unit,
    onReorder: (from: Int, to: Int) -> Unit
) {
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetY   by remember { mutableStateOf(0f) }
    val itemHeightPx  = with(LocalDensity.current) { 48.dp.toPx() }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 新增子项栏
            Row(
                modifier = Modifier
                    .fillMaxWidth().height(48.dp)
                    .background(Color(0xFF252525))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onAddClick() }) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, "新增子项", tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("新增子项", color = Color(0xFF007AFF), fontSize = 13.sp)
            }
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF333333))

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                    GlossaryChildRow(
                        name        = item.name,
                        isEditMode  = isEditMode,
                        isDragging  = draggingIndex == index,
                        dragOffsetY = if (draggingIndex == index) dragOffsetY else 0f,
                        onClick     = { onItemClick(item) },
                        onLongPress = { onItemLongPress(item) },
                        onDragStart = if (isEditMode) { offsetY ->
                            draggingIndex = index; dragOffsetY = offsetY
                        } else null,
                        onDragMove = if (isEditMode) { offsetY ->
                            dragOffsetY = offsetY
                        } else null,
                        onDragEnd = if (isEditMode) {
                            {
                                if (draggingIndex >= 0) {
                                    val steps = (dragOffsetY / itemHeightPx).toInt()
                                    val target = (draggingIndex + steps).coerceIn(0, items.size - 1)
                                    if (target != draggingIndex) onReorder(draggingIndex, target)
                                }
                                draggingIndex = -1; dragOffsetY = 0f
                            }
                        } else null
                    )
                }
                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("暂无子项，点击上方新增", color = Color(0xFF666666), fontSize = 11.sp) }
                    }
                }
            }
        }

        // FAB：查看模式显示 Edit 图标，编辑模式显示 Check 图标
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
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
 * 子项行 — 与父项行相同的手势逻辑
 * 查看模式：短按插入，长按详情
 * 编辑模式：短按删除确认，长按500ms拖拽
 */
@Composable
private fun GlossaryChildRow(
    name: String,
    isEditMode: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: ((Float) -> Unit)? = null,
    onDragMove: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = with(density) { dragOffsetY.toDp() })
            .background(if (isDragging) Color(0xFF007AFF).copy(alpha = 0.15f) else Color.Transparent)
            .pointerInput(isEditMode) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var longPressed = false
                        var totalOffsetY = 0f
                        val startTime = System.currentTimeMillis()

                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            val elapsed = System.currentTimeMillis() - startTime
                            val change = event.changes.firstOrNull() ?: break

                            if (!longPressed && elapsed >= 500L) {
                                longPressed = true
                                if (isEditMode && onDragStart != null) {
                                    onDragStart(totalOffsetY)
                                } else {
                                    onLongPress()
                                }
                            }

                            if (longPressed && isEditMode && onDragMove != null) {
                                totalOffsetY += change.positionChange().y
                                onDragMove(totalOffsetY)
                                change.consume()
                            }

                            event = awaitPointerEvent()
                        }

                        if (longPressed) {
                            onDragEnd?.invoke()
                        } else {
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed < 500L) onClick()
                        }
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text     = name,
            color    = Color(0xFFE0E0E0),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── 对话框 ───────────────────────────────────────────────────────────────────

@Composable
private fun GlossaryAddDialog(
    title: String,
    duplicateMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF252525)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, color = Color(0xFFE0E0E0), fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("请输入名称", color = Color(0xFF666666)) },
                    textStyle     = androidx.compose.ui.text.TextStyle(color = Color(0xFFE0E0E0), fontSize = 16.sp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF007AFF),
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor          = Color(0xFF007AFF)
                    ),
                    singleLine = true
                )

                // 重复提示（显示在对话框内，不被键盘遮挡）
                if (duplicateMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(8.dp),
                        color    = Color(0xFF8B0000).copy(alpha = 0.8f)
                    ) {
                        Text(
                            text     = duplicateMessage,
                            color    = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF888888)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(name) }) { Text("确定", color = Color(0xFF007AFF)) }
                }
            }
        }
    }
}

@Composable
private fun GlossaryDetailDialog(item: GlossaryItem, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF252525)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(item.name, color = Color(0xFFE0E0E0), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("关闭", color = Color(0xFF007AFF)) }
                }
            }
        }
    }
}
