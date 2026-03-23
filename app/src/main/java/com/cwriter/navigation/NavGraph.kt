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
        // 首页
        composable(Screen.Home.route) {
            HomeScreen(
                userId = userId,
                onNavigateToCreateWork = {
                    navController.navigate(Screen.CreateWork.route)
                },
                onNavigateToChapters = { workId ->
                    navController.navigate(Screen.Chapters.createRoute(workId))
                }
            )
        }

        // 创建作品页
        composable(Screen.CreateWork.route) {
            CreateWorkScreen(
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onWorkCreated = {
                    navController.popBackStack()
                }
            )
        }

        // 章节列表页
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
                }
            )
        }
    }
}
