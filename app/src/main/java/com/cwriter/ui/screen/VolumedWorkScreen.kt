package com.cwriter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.Volume
import com.cwriter.data.model.Chapter
import com.cwriter.ui.theme.DarkPrimary
import com.cwriter.ui.viewmodel.VolumedWorkViewModel
import com.cwriter.ui.components.CatalogPanel
import com.cwriter.ui.components.VolumeActionMenu
import com.cwriter.ui.components.ChapterActionMenu

/**
 * 分卷作品管理页面 - 从 Vue3 代码移植
 * 功能：展示卷和章节列表，支持创建/删除卷和章节
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumedWorkScreen(
    userId: String,
    workId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String, String, String) -> Unit, // workId, chapterId, volumeId
    viewModel: VolumedWorkViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // 状态订阅
    val workInfo by viewModel.workInfo.collectAsState()
    val volumes by viewModel.volumes.collectAsState()
    val chaptersByVolume by viewModel.chaptersByVolume.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val expandedVolumeId by viewModel.expandedVolumeId.collectAsState()
    
    // 本地 UI 状态
    var showCatalog by remember { mutableStateOf(false) }
    var showCreateChapterModal by remember { mutableStateOf(false) }
    var showCreateVolumeModal by remember { mutableStateOf(false) }
    var showRenameVolumeModal by remember { mutableStateOf(false) }
    var showVolumeMenu by remember { mutableStateOf(false) }
    var currentEditingVolume by remember { mutableStateOf<Volume?>(null) }
    
    // 初始化
    LaunchedEffect(userId, workId) {
        viewModel.init(context, userId, workId)
    }
    
    // 监听 ViewModel 事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VolumedWorkViewModel.Event.ShowToast -> {
                    // 显示 Toast
                }
                is VolumedWorkViewModel.Event.NavigateToEditor -> {
                    onNavigateToEditor(event.workId, event.chapterId, event.volumeId)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(workInfo.title ?: "加载中...") 
                },
                navigationIcon = {
                    // 左侧菜单图标
                    IconButton(onClick = { showCatalog = !showCatalog }) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(2.dp)
                                        .background(Color(0xFFB3B3B3))
                                )
                            }
                        }
                    }
                },
                actions = {
                    // 右侧关闭按钮
                    TextButton(onClick = onNavigateBack) {
                        Text(
                            text = "X",
                            fontSize = 16.sp,
                            fontWeight = FontWeight(500),
                            color = Color(0xFFB3B3B3)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            // FAB 按钮
            FloatingActionButton(
                onClick = { 
                    if (volumes.isEmpty()) {
                        showCreateVolumeModal = true
                    } else {
                        showCreateChapterModal = true
                    }
                },
                containerColor = Color(0xFFFF6B35),
                modifier = Modifier
                    .size(56.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 32.sp,
                    fontWeight = FontWeight(300),
                    color = Color.White
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 主内容区域
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 头部统计栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "章节列表",
                        fontSize = 18.sp,
                        fontWeight = FontWeight(600),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 章节总数
                        Text(
                            text = "共 ${volumes.sumOf { chaptersByVolume[it.id]?.size ?: 0 }} 章",
                            fontSize = 14.sp,
                            color = Color(0xFFB3B3B3)
                        )
                        
                        // 排序切换
                        Surface(
                            modifier = Modifier.clickable { 
                                viewModel.toggleSortOrder()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFF6B35).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (sortOrder == "asc") "正序" else "倒序",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF6B35),
                                    fontWeight = FontWeight(500)
                                )
                                Text(
                                    text = "↓",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF6B35),
                                    modifier = Modifier.rotate(if (sortOrder == "desc") 180f else 0f)
                                )
                            }
                        }
                    }
                }
                
                // 内容区域
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (volumes.isEmpty()) {
                    // 空状态
                    EmptyVolumesState(
                        onCreateClick = { showCreateVolumeModal = true }
                    )
                } else {
                    // 卷列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 新增卷按钮
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCreateVolumeModal = true },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF6B35).copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp, 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                Color(0xFFFF6B35).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            fontSize = 20.sp,
                                            color = Color(0xFFFF6B35),
                                            fontWeight = FontWeight(300)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Text(
                                        text = "新增卷",
                                        fontSize = 15.sp,
                                        color = Color(0xFFFF6B35),
                                        fontWeight = FontWeight(500)
                                    )
                                }
                            }
                        }
                        
                        // 卷列表
                        items(volumes) { volume ->
                            VolumeItem(
                                volume = volume,
                                chapters = chaptersByVolume[volume.id] ?: emptyList(),
                                isExpanded = expandedVolumeId == volume.id,
                                sortOrder = sortOrder,
                                onToggleExpand = { 
                                    viewModel.toggleVolumeExpand(volume.id)
                                },
                                onVolumeLongClick = { 
                                    currentEditingVolume = volume
                                    showVolumeMenu = true
                                },
                                onChapterClick = { chapter ->
                                    viewModel.openChapter(chapter)
                                },
                                onChapterLongClick = { chapter ->
                                    // 显示章节操作菜单
                                },
                                onLoadChapters = {
                                    viewModel.loadVolumeChapters(volume.id)
                                }
                            )
                        }
                    }
                }
            }
            
            // 目录栏
            if (showCatalog) {
                CatalogPanel(
                    isVisible = showCatalog,
                    onDismiss = { showCatalog = false },
                    onOpenChapter = { chapter ->
                        viewModel.openChapter(chapter)
                    }
                )
            }
            
            // 卷操作菜单
            if (showVolumeMenu && currentEditingVolume != null) {
                VolumeActionMenu(
                    onDismiss = { showVolumeMenu = false },
                    onRename = {
                        showVolumeMenu = false
                        showRenameVolumeModal = true
                    },
                    onDelete = {
                        showVolumeMenu = false
                        viewModel.confirmDeleteVolume(currentEditingVolume!!)
                    }
                )
            }
        }
    }
}

/**
 * 空状态组件
 */
@Composable
fun EmptyVolumesState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp, 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 这里应该有一个图标，但为简化暂时用文本
        Text(
            text = "📚",
            fontSize = 64.sp,
            modifier = Modifier.alpha(0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "还没有创建卷",
            fontSize = 16.sp,
            color = Color(0xFFB3B3B3),
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        Surface(
            modifier = Modifier.clickable(onClick = onCreateClick),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFF6B35)
        ) {
            Text(
                text = "创建第一个卷",
                modifier = Modifier.padding(12.dp, 24.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight(500),
                color = Color.White
            )
        }
    }
}

/**
 * 卷列表项组件
 */
@Composable
fun VolumeItem(
    volume: Volume,
    chapters: List<Chapter>,
    isExpanded: Boolean,
    sortOrder: String,
    onToggleExpand: () -> Unit,
    onVolumeLongClick: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onChapterLongClick: (Chapter) -> Unit,
    onLoadChapters: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF2D2D2D).copy(alpha = 0.6f)
    ) {
        Column {
            // 卷头
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                // TODO: 添加长按事件
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 展开图标
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isExpanded) 90f else 0f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            fontSize = 18.sp,
                            color = Color(0xFFB3B3B3),
                            fontWeight = FontWeight(300)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 卷标题
                    Text(
                        text = volume.name ?: "未命名卷",
                        fontSize = 15.sp,
                        fontWeight = FontWeight(600),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 章节数量
                    Text(
                        text = "${chapters.size}章",
                        fontSize = 13.sp,
                        color = Color(0xFFB3B3B3)
                    )
                }
            }
            
            // 卷内章节（可展开）
            if (isExpanded) {
                if (chapters.isEmpty() && !viewModel.isVolumeLoaded(volume.id)) {
                    // 加载中
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    
                    // 触发加载
                    LaunchedEffect(Unit) {
                        onLoadChapters()
                    }
                } else {
                    // 章节列表
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val displayChapters = if (sortOrder == "desc") chapters.reversed() else chapters
                        
                        displayChapters.forEachIndexed { index, chapter ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChapterClick(chapter) }
                                // TODO: 添加长按事件
                                ,
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF323232).copy(alpha = 0.7f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp, 16.dp)
                                ) {
                                    // 章节标题
                                    Text(
                                        text = "第${viewModel.getChapterNumber(volume.id, index, sortOrder)}章 ${chapter.title ?: "未命名章节"}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight(500),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // 元信息
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "${chapter.wordCount}字",
                                            fontSize = 12.sp,
                                            color = Color(0xFF888888)
                                        )
                                        Text(
                                            text = viewModel.formatTime(chapter.updatedAt),
                                            fontSize = 12.sp,
                                            color = Color(0xFF888888)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // 状态
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (chapter.isCompleted) Color(0xFF4ECDC4) else Color(0xFFFF6B35)
                                    ) {
                                        Text(
                                            text = if (chapter.isCompleted) "已完成" else "写作中",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            color = Color.White
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

/**
 * 获取章节编号（全局连续编号）
 * 与 Vue 版本保持一致
 */
fun getChapterNumber(
    volumes: List<Volume>,
    chaptersByVolume: Map<String, List<Chapter>>,
    volumeId: String,
    chapterIndex: Int,
    sortOrder: String
): Int {
    var num = 1
    val currentVolumes = if (sortOrder == "desc") volumes.reversed() else volumes
    
    for (vol in currentVolumes) {
        if (vol.id == volumeId) {
            // 当前卷，加上章节在卷内的索引
            val chapters = chaptersByVolume[volumeId] ?: emptyList()
            val actualIndex = if (sortOrder == "desc") chapters.size - 1 - chapterIndex else chapterIndex
            return num + actualIndex
        }
        // 还没到当前卷，累加前面卷的章节数
        num += (chaptersByVolume[vol.id]?.size ?: 0)
    }
    
    return num + chapterIndex
}
