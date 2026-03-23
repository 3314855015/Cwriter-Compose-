package com.cwriter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.UserStats
import com.cwriter.data.model.Work
import com.cwriter.ui.theme.DarkPrimary
import com.cwriter.ui.theme.NavBarBackground
import com.cwriter.ui.viewmodel.HomeViewModel
import com.cwriter.ui.components.CreateWorkDialog

/**
 * 首页 Screen - MVVM 架构
 * View 层：通过 StateFlow 订阅 ViewModel 状态，单向数据流
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: String,
    onNavigateToChapters: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current

    // StateFlow → Compose State，自动订阅更新
    val works by viewModel.works.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 本地 UI 状态
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    // 初始化 ViewModel
    LaunchedEffect(userId) {
        viewModel.init(context, userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("CWriter", fontWeight = FontWeight.Bold) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = DarkPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 统计卡片
            StatsCard(stats = stats)

            // 标签栏
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                listOf("最近", "收藏", "全部").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 作品列表
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (works.isEmpty()) {
                EmptyState(onCreateClick = { showCreateDialog = true })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val filteredWorks = when (selectedTab) {
                        1 -> works.filter { it.isFavorite }
                        else -> works
                    }
                    items(filteredWorks) { work ->
                        WorkItem(
                            work = work,
                            onClick = { onNavigateToChapters(work.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(work) }
                        )
                    }
                }
            }
        }
    }

    // 创建作品对话框（使用 components/CreateWorkDialog.kt）
    if (showCreateDialog) {
        CreateWorkDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, description, structureType ->
                viewModel.createWork(title, description, structureType)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun StatsCard(stats: UserStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "作品", value = stats.totalWorks.toString())
            StatItem(label = "字数", value = formatNumber(stats.totalWords))
            StatItem(label = "地图", value = stats.totalMaps.toString())
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkPrimary
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun WorkItem(
    work: Work,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = work.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (work.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (work.isFavorite) DarkPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            if (work.description.isNotEmpty()) {
                Text(
                    text = work.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${work.chapterCount}章 · ${formatNumber(work.wordCount.toLong())}字",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = work.getFormattedTime(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun EmptyState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EditNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有作品",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮开始创作",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkPrimary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建第一部作品")
        }
    }
}

fun formatNumber(number: Long): String {
    return when {
        number >= 10000 -> String.format("%.1f万", number / 10000.0)
        else -> number.toString()
    }
}
