package com.cwriter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cwriter.ui.screen.*

@Composable
fun CWriterNavGraph(
    navController: NavHostController,
    userId: String = "default_user"
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // 首页（带底部导航的主屏幕，创建作品改为模态框，不再跳转页面）
        composable(Screen.Home.route) {
            MainScreen(
                navController = navController,
                userId = userId
            )
        }

        // 分卷作品管理页
        composable(
            route = Screen.VolumedWork.route,
            arguments = listOf(
                navArgument("workId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            VolumedWorkScreen(
                userId = userId,
                workId = workId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { workId, chapterId, volumeId ->
                    navController.navigate(Screen.ChapterEditor.createRoute(workId, chapterId))
                }
            )
        }

        // 章节列表页（旧版，保留兼容）
        composable(
            route = Screen.Chapters.route,
            arguments = listOf(
                navArgument("workId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            ChaptersScreen(
                userId = userId,
                workId = workId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { workId, chapterId ->
                    navController.navigate(Screen.ChapterEditor.createRoute(workId, chapterId))
                }
            )
        }

        // 章节编辑页
        composable(
            route = Screen.ChapterEditor.route,
            arguments = listOf(
                navArgument("workId") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            ChapterEditorScreen(
                userId = userId,
                workId = workId,
                chapterId = chapterId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChapter = { newChapterId ->
                    // 替换当前页面（上一章/下一章导航）
                    navController.navigate(Screen.ChapterEditor.createRoute(workId, newChapterId)) {
                        popUpTo(Screen.ChapterEditor.createRoute(workId, chapterId)) { inclusive = true }
                    }
                }
            )
        }
    }
}
