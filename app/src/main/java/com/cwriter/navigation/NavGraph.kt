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
    userId: String = "default_user",
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            MainScreen(
                navController = navController,
                userId = userId,
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )
        }

        composable(
            route = Screen.VolumedWork.route,
            arguments = listOf(navArgument("workId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            VolumedWorkScreen(
                userId = userId,
                workId = workId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { wId, chapterId, _ ->
                    navController.navigate(Screen.ChapterEditor.createRoute(wId, chapterId))
                }
            )
        }

        composable(
            route = Screen.Chapters.route,
            arguments = listOf(navArgument("workId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            ChaptersScreen(
                userId = userId,
                workId = workId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { wId, chapterId ->
                    navController.navigate(Screen.ChapterEditor.createRoute(wId, chapterId))
                }
            )
        }

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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChapter = { newChapterId ->
                    navController.navigate(Screen.ChapterEditor.createRoute(workId, newChapterId)) {
                        popUpTo(Screen.ChapterEditor.createRoute(workId, chapterId)) { inclusive = true }
                    }
                }
            )
        }
    }
}
