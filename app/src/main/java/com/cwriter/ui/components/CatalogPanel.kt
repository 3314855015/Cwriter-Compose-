package com.cwriter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Volume
import com.cwriter.ui.viewmodel.VolumedWorkViewModel

/**
 * 目录栏组件
 * 左侧滑出，占屏幕 3/4 宽度
 * 两栏布局：索引栏 (1/3) + 内容栏 (2/3)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    viewModel: VolumedWorkViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val volumes by viewModel.volumes.collectAsState()
    val chaptersByVolume by viewModel.chaptersByVolume.collectAsState()
    var selectedVolumeId by remember { mutableStateOf<String?>(null) }
    
    // 默认选中第一个卷
    LaunchedEffect(volumes) {
        if (selectedVolumeId == null && volumes.isNotEmpty()) {
            selectedVolumeId = volumes[0].id
        }
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val panelWidth = screenWidth * 3 / 4
    val offsetAnimation by animateFloatAsState(
        targetValue = if (isVisible) 0f else -1f,
        label = "catalog_offset"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                if (isVisible) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                }
            }
    ) {
        // 半透明遮罩
        val overlayAlpha = if (isVisible) 0.5f else 0f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(overlayAlpha)
                .background(Color.Black)
        )
        
        // 目录面板
        Surface(
            modifier = Modifier
                .width(panelWidth)
                .fillMaxHeight()
                .offset(x = panelWidth * offsetAnimation)
                .pointerInput(Unit) {
                    // 阻止面板内部的点击事件冒泡到遮罩层
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 索引栏 (1/3)
                VolumeIndexColumn(
                    volumes = volumes,
                    selectedVolumeId = selectedVolumeId,
                    onVolumeSelected = onVolumeSelected,
                    modifier = Modifier.weight(1f)
                )
                
                // 内容栏 (2/3)
                ChapterContentColumn(
                    volumes = volumes,
                    volumeChapters = chaptersByVolume,
                    selectedVolumeId = selectedVolumeId,
                    onChapterClick = { chapterId ->
                        val chapter = chaptersByVolume.values.flatten().find { it.id == chapterId }
                        if (chapter != null) {
                            onOpenChapter(chapter)
                        }
                    },
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

/**
 * 索引栏 - 显示卷列表
 */
@Composable
private fun VolumeIndexColumn(
    volumes: List<Volume>,
    selectedVolumeId: String?,
    onVolumeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(volumes) { volume ->
                VolumeIndexItem(
                    volume = volume,
                    isSelected = volume.id == selectedVolumeId,
                    onClick = { onVolumeSelected(volume.id) }
                )
            }
        }
    }
}

@Composable
private fun VolumeIndexItem(
    volume: Volume,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
               else Color.Transparent,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp)
        ) {
            Text(
                text = volume.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${volume.chapterCount}章",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 内容栏 - 显示选中卷的章节列表
 */
@Composable
private fun ChapterContentColumn(
    volumes: List<Volume>,
    volumeChapters: Map<String, List<Chapter>>,
    selectedVolumeId: String?,
    onChapterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "章节列表",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Divider()
            
            // 章节列表
            val selectedVolume = volumes.find { it.id == selectedVolumeId }
            if (selectedVolume == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "请选择左侧的卷",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                val chapters = volumeChapters[selectedVolumeId] ?: emptyList()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chapters) { chapter ->
                        ChapterContentItem(
                            chapter = chapter,
                            onClick = { onChapterClick(chapter.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterContentItem(
    chapter: Chapter,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = chapter.title,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
