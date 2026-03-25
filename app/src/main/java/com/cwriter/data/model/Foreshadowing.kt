package com.cwriter.data.model

import java.util.UUID

/**
 * 伏笔状态
 */
enum class ForeshadowingStatus {
    PENDING,    // 待回收（只埋未收）
    RECYCLED    // 已回收
}

/**
 * 伏笔数据模型
 */
data class Foreshadowing(
    val id: String = UUID.randomUUID().toString(),
    val workId: String = "",
    val chapterId: String = "",
    val createdParagraphIndex: Int = 0,    // 创建伏笔的段落索引
    val content: String = "",              // 伏笔内容
    val status: ForeshadowingStatus = ForeshadowingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val recycledChapterId: String? = null,  // 回收时的章节ID
    val recycledParagraphIndex: Int? = null, // 回收伏笔的段落索引
    val recycledAt: Long? = null            // 回收时间
) {
    fun getFormattedTime(): String {
        val date = java.util.Date(createdAt)
        val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    fun getRecycledFormattedTime(): String? {
        val time = recycledAt ?: return null
        val date = java.util.Date(time)
        val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}

/**
 * 段落伏笔统计
 */
data class ParagraphForeshadowing(
    val paragraphIndex: Int,
    val totalCount: Int = 0,             // 总伏笔数（埋下 + 回收）
    val pendingCount: Int = 0,           // 待回收数（在该段落创建且未回收）
    val recycledCount: Int = 0,          // 已回收数（在该段落创建且已回收）
    val recycledHereCount: Int = 0       // 在该段落回收的伏笔数（不论创建位置）
) {
    /**
     * 获取显示颜色和数字
     * @return Pair<颜色, 数字>
     */
    fun getDisplayInfo(): Pair<String, Int> {
        return when {
            pendingCount > 0 && recycledHereCount > 0 -> "dark_blue" to (pendingCount + recycledHereCount)
            pendingCount > 0 -> "light_gray" to pendingCount
            recycledHereCount > 0 -> "light_blue" to recycledHereCount
            recycledCount > 0 -> "light_blue" to recycledCount  // 兼容旧逻辑
            else -> "none" to 0
        }
    }
}
