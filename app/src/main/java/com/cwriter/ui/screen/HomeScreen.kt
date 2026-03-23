package com.cwriter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.UserStats
import com.cwriter.data.model.Work
import com.cwriter.ui.theme.AccentOrange
import com.cwriter.ui.theme.CWriterTheme
import com.cwriter.ui.components.CreateWorkDialog
import com.cwriter.ui.viewmodel.HomeViewModel

// ===== 主题色值（来自 UniApp themeManager.js）=====
private val HomeBgDark             = Color(0xFF1A1A1A)
private val HomeBgLight            = Color(0xFFF5F5F5)
private val HomeCardDark           = Color(0xFF2D2D2D)
private val HomeCardLight          = Color(0xFFFFFFFF)
private val HomeStatsBgDark        = Color(0xFF404040)
private val HomeStatsBgLight       = Color(0xFFF0F0F0)
private val HomeTextPrimaryDark    = Color(0xFFFFFFFF)
private val HomeTextPrimaryLight   = Color(0xFF333333)
private val HomeTextSecondaryDark  = Color(0xFFB3B3B3)
private val HomeTextSecondaryLight = Color(0xFF666666)
private val HomeTextTertiaryDark   = Color(0xFF808080)
private val HomeTextTertiaryLight  = Color(0xFF999999)
private val HomeDividerDark        = Color(0xFF404040)
private val HomeDividerLight       = Color(0xFFEEEEEE)

@Composable
fun HomeScreen(
    userId: String,
    isDark: Boolean = false,
    onNavigateToChapters: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val works   by viewModel.works.collectAsState()
    val stats   by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab      by remember { mutableIntStateOf(0) }

    // 动态颜色（根据 isDark 选择 UniApp 色值）
    val homeBg        = if (isDark) HomeBgDark            else HomeBgLight
    val homeCard      = if (isDark) HomeCardDark           else HomeCardLight
    val homeStatsBg   = if (isDark) HomeStatsBgDark        else HomeStatsBgLight
    val textPrimary   = if (isDark) HomeTextPrimaryDark    else HomeTextPrimaryLight
    val textSecondary = if (isDark) HomeTextSecondaryDark  else HomeTextSecondaryLight
    val textTertiary  = if (isDark) HomeTextTertiaryDark   else HomeTextTertiaryLight
    val divider       = if (isDark) HomeDividerDark        else HomeDividerLight

    LaunchedEffect(userId) { viewModel.init(context, userId) }

    val tabs = listOf("最近", "收藏", "本机", "场景")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(homeBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            UserHeader(
                cardBg        = homeCard,
                textPrimary   = textPrimary,
                textSecondary = textSecondary
            )

            StatsRow(
                stats         = stats,
                statsBg       = homeStatsBg,
                textSecondary = textSecondary,
                divider       = divider
            )

            TabBar(
                tabs          = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                cardBg        = homeCard,
                textSecondary = textSecondary,
                divider       = divider
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentOrange, strokeWidth = 3.dp)
                        }
                    }
                    else -> {
                        val filtered = when (selectedTab) {
                            1    -> works.filter { it.isFavorite }
                            else -> works
                        }
                        if (filtered.isEmpty()) {
                            EmptyWorksState(
                                onCreateClick = { showCreateDialog = true },
                                textTertiary  = textTertiary
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 14.dp, end = 14.dp,
                                    top = 10.dp, bottom = 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered) { work ->
                                    WorkListItem(
                                        work         = work,
                                        onClick      = { onNavigateToChapters(work.id) },
                                        cardBg       = homeCard,
                                        textPrimary  = textPrimary,
                                        textTertiary = textTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick        = { showCreateDialog = true },
            containerColor = AccentOrange,
            shape          = CircleShape,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .size(52.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "创建作品",
                tint               = Color.White,
                modifier           = Modifier.size(24.dp)
            )
        }
    }

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

// ===== 用户信息区 =====
@Composable
private fun UserHeader(
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF4ECDC4)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "游客",
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textPrimary
            )
            Text(
                text     = "继续你的创作之旅",
                fontSize = 12.sp,
                color    = textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEEEEE))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "通知",
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ===== 统计区 =====
@Composable
private fun StatsRow(
    stats: UserStats,
    statsBg: Color,
    textSecondary: Color,
    divider: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(statsBg)
    ) {
        HorizontalDivider(color = divider, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCell(value = stats.totalWorks.toString(), label = "总作品",
                valueColor = AccentOrange, labelColor = textSecondary)
            StatCell(value = formatWordCount(stats.totalWords), label = "总字数",
                valueColor = Color(0xFF4ECDC4), labelColor = textSecondary)
            StatCell(value = stats.totalMaps.toString(), label = "地图",
                valueColor = Color(0xFF4ECDC4), labelColor = textSecondary)
        }
        HorizontalDivider(color = divider, thickness = 0.5.dp)
    }
}

@Composable
private fun StatCell(value: String, label: String, valueColor: Color, labelColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(text = label, fontSize = 12.sp, color = labelColor, modifier = Modifier.padding(top = 2.dp))
    }
}

// ===== Tab 栏 =====
@Composable
private fun TabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    cardBg: Color,
    textSecondary: Color,
    divider: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(index) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text       = title,
                    fontSize   = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) AccentOrange else textSecondary
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(AccentOrange, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
    HorizontalDivider(color = divider, thickness = 0.5.dp)
}

// ===== 作品列表项（卡片式）=====
@Composable
private fun WorkListItem(
    work: Work,
    onClick: () -> Unit,
    cardBg: Color,
    textPrimary: Color,
    textTertiary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：标题 + 修改时间
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = work.title,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text     = "${work.getFormattedTime()}修改",
                fontSize = 12.sp,
                color    = textTertiary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧：章数 + 文件图标 + 字数
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "${work.chapterCount}章", fontSize = 13.sp, color = textTertiary)
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Text(text = "${formatWordCount(work.wordCount.toLong())}字", fontSize = 13.sp, color = textTertiary)
        }
    }
}

// ===== 空状态 =====
@Composable
private fun EmptyWorksState(onCreateClick: () -> Unit, textTertiary: Color) {
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
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFCCCCCC)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "还没有作品", fontSize = 16.sp, color = textTertiary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "点击右下角按钮开始创作", fontSize = 13.sp, color = Color(0xFFCCCCCC))
    }
}

// ===== 工具函数 =====
fun formatWordCount(count: Long): String = when {
    count >= 10000 -> String.format("%.1fw", count / 10000.0)
    count >= 1000  -> String.format("%.1fk", count / 1000.0)
    else           -> count.toString()
}

fun formatNumber(number: Long): String = formatWordCount(number)

// ===== Preview =====
@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "首页-亮色",
    device = "spec:width=390dp,height=844dp,dpi=460"
)
@Composable
fun HomeScreenLightPreview() {
    CWriterTheme(darkTheme = false) {
        HomeScreen(userId = "", isDark = false)
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "首页-深色",
    device = "spec:width=390dp,height=844dp,dpi=460"
)
@Composable
fun HomeScreenDarkPreview() {
    CWriterTheme(darkTheme = true) {
        HomeScreen(userId = "", isDark = true)
    }
}
