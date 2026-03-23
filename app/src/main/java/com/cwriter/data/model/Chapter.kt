package com.cwriter.data.model

import java.util.UUID

/**
 * 章节数据模型
 * volumeId: 关联的卷ID（分卷作品使用）
 */
data class Chapter(
    val id: String = System.currentTimeMillis().toString(),
    var title: String = "",
    var content: String = "",
    var wordCount: Int = 0,
    var isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var volumeId: String = ""  // 新增：关联的卷ID
) {
    fun getFormattedTime(): String {
        val date = java.util.Date(updatedAt)
        return java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(date)
    }

    fun updateWordCount() {
        wordCount = content.filter { !it.isWhitespace() }.length
    }
}

/**
 * 作品配置（包含作品信息和章节列表）
 */
data class WorkConfig(
    val work: Work,
    val chapters: List<Chapter> = emptyList()
)
