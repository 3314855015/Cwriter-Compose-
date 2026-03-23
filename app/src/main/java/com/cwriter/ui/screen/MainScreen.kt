package com.cwriter.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cwriter.navigation.Screen
import com.cwriter.ui.theme.AccentOrange
import com.cwriter.ui.theme.CWriterTheme
import com.cwriter.ui.theme.LocalIsDark

// 主题模式：只保留 UniApp 对应的两档
enum class ThemeMode { LIGHT, DARK }

private data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    NavItem("首页",  Icons.Filled.Home,      Icons.Outlined.Home),
    NavItem("主题",  Icons.Filled.WbSunny,   Icons.Outlined.WbSunny),   // icon 会被动态替换
    NavItem("管理",  Icons.Filled.Dashboard,  Icons.Outlined.Dashboard),
    NavItem("服务",  Icons.Filled.MenuBook,   Icons.Outlined.MenuBook),
    NavItem("我的",  Icons.Filled.Person,     Icons.Outlined.Person),
)

// 导航栏颜色（亮/暗两套，来自 UniApp themeManager）
private val BottomNavBgLight       = Color(0xFFFFFFFF)
private val BottomNavBgDark        = Color(0xFF2D2D2D)
private val BottomNavBorderLight   = Color(0xFFEEEEEE)
private val BottomNavBorderDark    = Color(0xFF404040)
private val BottomNavUnselected    = Color(0xFF999999)

@Composable
fun MainScreen(
    navController: NavHostController,
    userId: String = "default_user"
) {
    val context = LocalContext.current

    // 主题状态：从持久化读取，默认跟随系统
    val systemDark = isSystemInDarkTheme()
    var themeMode by remember {
        mutableStateOf(loadThemeMode(context, systemDark))
    }
    val isDark = themeMode == ThemeMode.DARK

    var selectedTab by remember { mutableIntStateOf(0) }

    // 切换：LIGHT ↔ DARK（对应 UniApp toggleTheme）
    fun toggleTheme() {
        themeMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
        saveThemeMode(context, themeMode)
    }

    // 主题按钮图标：深色时显示月亮，浅色时显示太阳
    val themeIcon  = if (isDark) Icons.Filled.DarkMode  else Icons.Filled.WbSunny
    val themeLabel = if (isDark) "深色" else "浅色"

    // 导航栏动态颜色
    val bottomNavBg     = if (isDark) BottomNavBgDark     else BottomNavBgLight
    val bottomNavBorder = if (isDark) BottomNavBorderDark else BottomNavBorderLight

    // 用当前主题包裹整个 Scaffold
    CWriterTheme(darkTheme = isDark) {
        androidx.compose.runtime.CompositionLocalProvider(LocalIsDark provides isDark) {
        Scaffold(
            bottomBar = {
                Column {
                    HorizontalDivider(color = bottomNavBorder, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bottomNavBg)
                            .navigationBarsPadding()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            val icon = when (index) {
                                1    -> themeIcon
                                else -> if (isSelected) item.selectedIcon else item.unselectedIcon
                            }
                            val label = if (index == 1) themeLabel else item.title
                            val tint  = if (isSelected) AccentOrange else BottomNavUnselected

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        if (index == 1) toggleTheme()
                                        else selectedTab = index
                                    }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = label, fontSize = 10.sp, color = tint)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        userId = userId,
                        isDark = isDark,
                        onNavigateToChapters = { workId ->
                            navController.navigate(Screen.VolumedWork.createRoute(workId))
                        }
                    )
                    2 -> ManageScreen()
                    3 -> ServicePlaceholderScreen()
                    4 -> ProfilePlaceholderScreen()
                }
            }
        }
        } // CompositionLocalProvider
    }
}

// 持久化：保存 / 读取
fun saveThemeMode(context: Context, mode: ThemeMode) {
    context.getSharedPreferences("cwriter_prefs", Context.MODE_PRIVATE)
        .edit().putString("theme_mode", mode.name).apply()
}

fun loadThemeMode(context: Context, systemDark: Boolean): ThemeMode {
    val saved = context.getSharedPreferences("cwriter_prefs", Context.MODE_PRIVATE)
        .getString("theme_mode", null)
    return when (saved) {
        ThemeMode.LIGHT.name -> ThemeMode.LIGHT
        ThemeMode.DARK.name  -> ThemeMode.DARK
        else -> if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT  // 首次启动跟随系统
    }
}

@Composable
fun ServicePlaceholderScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("服务功能开发中", color = Color(0xFF999999))
    }
}

@Composable
fun ProfilePlaceholderScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("个人中心开发中", color = Color(0xFF999999))
    }
}
