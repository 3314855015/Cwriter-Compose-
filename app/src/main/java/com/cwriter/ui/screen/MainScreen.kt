package com.cwriter.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cwriter.navigation.Screen
import com.cwriter.ui.theme.AccentOrange
import com.cwriter.ui.theme.DarkSurface

// 底部导航项数据类
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    object Theme : BottomNavItem(
        route = "theme",
        title = "主题",
        selectedIcon = Icons.Filled.DarkMode,
        unselectedIcon = Icons.Outlined.DarkMode
    )
    object Manage : BottomNavItem(
        route = "manage",
        title = "管理",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )
    object Service : BottomNavItem(
        route = "service",
        title = "服务",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    )
    object Profile : BottomNavItem(
        route = "profile",
        title = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

// 主题模式
enum class ThemeMode {
    LIGHT,      // 浅色
    DARK,       // 深色
    SYSTEM      // 跟随系统
}

@Composable
fun MainScreen(
    navController: NavHostController,
    userId: String = "default_user",
    currentThemeMode: ThemeMode = ThemeMode.DARK,
    onThemeChange: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Theme,
        BottomNavItem.Manage,
        BottomNavItem.Service,
        BottomNavItem.Profile
    )
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // 获取主题模式对应的图标和文字
    val themeInfo = when (currentThemeMode) {
        ThemeMode.LIGHT -> Pair(Icons.Filled.WbSunny, "浅色")
        ThemeMode.DARK -> Pair(Icons.Filled.DarkMode, "深色")
        ThemeMode.SYSTEM -> Pair(Icons.Filled.BrightnessAuto, "跟随")
    }
    
    // 循环切换到下一个主题
    fun cycleTheme() {
        val newMode = when (currentThemeMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        onThemeChange(newMode)
        // 保存设置
        saveThemeMode(context, newMode)
    }
    
    Scaffold(
        bottomBar = {
            // 自定义底部导航栏（增加底部padding避免被系统手势条遮挡）
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItems.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            // 主题按钮特殊处理：使用当前主题图标
                            val icon = if (item is BottomNavItem.Theme) {
                                themeInfo.first
                            } else if (isSelected) {
                                item.selectedIcon
                            } else {
                                item.unselectedIcon
                            }
                            // 主题按钮文字
                            val label = if (item is BottomNavItem.Theme) {
                                themeInfo.second
                            } else {
                                item.title
                            }
                            
                            // 发光效果修饰符（仅选中时）
                            val glowModifier = if (isSelected) {
                                Modifier.glowEffect()
                            } else {
                                Modifier
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        if (item is BottomNavItem.Theme) {
                                            cycleTheme()
                                        } else {
                                            selectedTab = index
                                        }
                                    }
                                    .then(glowModifier)
                                    .padding(vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    // 系统手势栏安全区域
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 根据选中标签显示对应内容
            when (selectedTab) {
                0, 1 -> HomeScreen( // 主题按钮不切换页面
                    userId = userId,
                    onNavigateToChapters = { workId ->
                        navController.navigate(Screen.Chapters.createRoute(workId))
                    }
                )
                2 -> ManagePlaceholderScreen()
                3 -> ServicePlaceholderScreen()
                4 -> ProfilePlaceholderScreen()
            }
        }
    }
}

// 发光效果修饰符
private fun Modifier.glowEffect(): Modifier = this.drawBehind {
    // 绘制白色渐变发光
    drawIntoCanvas { canvas ->
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // 底部发光
        val brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY + 10f),
            radius = size.width * 0.8f
        )
        
        drawRect(
            brush = brush,
            size = size
        )
    }
}

// 保存主题模式到 SharedPreferences
private fun saveThemeMode(context: Context, mode: ThemeMode) {
    val prefs = context.getSharedPreferences("cwriter_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("theme_mode", mode.name).apply()
}

// 从 SharedPreferences 读取主题模式
fun loadThemeMode(context: Context): ThemeMode {
    val prefs = context.getSharedPreferences("cwriter_prefs", Context.MODE_PRIVATE)
    val savedMode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
    return try {
        ThemeMode.valueOf(savedMode ?: ThemeMode.SYSTEM.name)
    } catch (e: Exception) {
        ThemeMode.SYSTEM
    }
}

// 占位页面
@Composable
fun ManagePlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("管理功能开发中", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun ServicePlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("服务功能开发中", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun ProfilePlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("个人中心开发中", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
