package com.cwriter.data.model

import java.util.UUID

/**
 * 作品数据模型
 */
data class Work(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val category: String = "novel",
    val structureType: StructureType = StructureType.VOLUMED,
    var wordCount: Int = 0,
    var chapterCount: Int = 0,
    var isFavorite: Boolean = false,
    var mapCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isActive: Boolean = true,
    // ====== 同步相关字段（APP间同步协议 v1.0）======
    var syncId: String = "",          // UUID，首次导出时生成，永不改变。作为 Reading 端的 bookId
    var syncVersion: Int = 0          // 每次有任何改动时自增，用于增量同步版本控制
) {
    enum class StructureType {
        SINGLE,      // 整体作品
        VOLUMED  // 分卷作品
    }

    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - updatedAt
        return when {
            diff < 60000 -> "刚刚"
            diff < 3600000 -> "${diff / 60000}分钟前"
            diff < 86400000 -> "${diff / 3600000}小时前"
            diff < 604800000 -> "${diff / 86400000}天前"
            else -> {
                val date = java.util.Date(updatedAt)
                java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(date)
            }
        }
    }
}

/**
 * 用户统计数据
 */
data class UserStats(
    val totalWorks: Int = 0,
    val totalWords: Long = 0,
    val totalMaps: Int = 0
)
