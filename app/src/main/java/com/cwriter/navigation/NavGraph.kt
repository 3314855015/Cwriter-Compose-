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
                onNavigateToEditor = { wId, chapterId, volumeId ->
                    navController.navigate(Screen.ChapterEditor.createRoute(wId, chapterId, volumeId))
                },
                onNavigateToSync = { wId ->
                    navController.navigate(Screen.Sync.createRoute(wId))
                }
            )
        }

        composable(
            route = Screen.Sync.route,
            arguments = listOf(navArgument("workId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            SyncScreen(
                userId = userId,
                workId = workId,
                onNavigateBack = { navController.popBackStack() }
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
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("volumeId") { type = NavType.StringType; defaultValue = "_" }
            )
        ) { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val volumeId = backStackEntry.arguments?.getString("volumeId")?.let {
                if (it == "_") "" else it
            } ?: ""
            ChapterEditorScreen(
                userId = userId,
                workId = workId,
                chapterId = chapterId,
                volumeId = volumeId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChapter = { newChapterId, newVolumeId ->
                    navController.navigate(Screen.ChapterEditor.createRoute(workId, newChapterId, newVolumeId)) {
                        popUpTo(Screen.ChapterEditor.createRoute(workId, chapterId, volumeId)) { inclusive = true }
                    }
                }
            )
        }
    }
}
