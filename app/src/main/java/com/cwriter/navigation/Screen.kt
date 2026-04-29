package com.cwriter.navigation

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chapters : Screen("chapters/{workId}") {
        fun createRoute(workId: String) = "chapters/$workId"
    }
    object VolumedWork : Screen("volumed_work/{workId}") {
        fun createRoute(workId: String) = "volumed_work/$workId"
    }
    object ChapterEditor : Screen("editor/{workId}/{chapterId}/{volumeId}") {
        fun createRoute(workId: String, chapterId: String, volumeId: String = "") =
            "editor/$workId/$chapterId/${volumeId.ifEmpty { "_" }}"
    }
    /** 同步到阅读APP页面 */
    object Sync : Screen("sync/{workId}") {
        fun createRoute(workId: String) = "sync/$workId"
    }
}
