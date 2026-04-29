package com.cwriter.data.model

import java.security.MessageDigest
import java.util.UUID

/**
 * APP间同步协议 v1.0 — Cwriter → Reading
 * 定义在 sync-app-spec.md 中，双方必须保持一致
 */

/**
 * 同步载荷顶层结构
 */
data class SyncPayload(
    val bookId: String,                    // Work.syncId (UUID)
    val bookTitle: String,
    val author: String = "",               // 可选，Cwriter 暂无作者概念
    val description: String = "",
    val syncVersion: Int,                  // Work.syncVersion
    val lastModified: Long,                // 时间戳
    val chapters: List<ChapterSyncItem>
)

/**
 * 章节同步单元
 */
data class ChapterSyncItem(
    val chapterId: String,                 // Chapter.syncId (UUID)
    val chapterIndex: Int,                 // 排序索引（从 0 开始）
    val title: String,
    val content: String,
    val contentHash: String,               // MD5(content)
    val volumeName: String? = null         // 分卷名，可选
)

/**
 * 同步结果（Reading 端返回）
 */
data class ImportResult(
    val isNewBook: Boolean,
    val bookTitle: String,
    val addedChapters: Int = 0,
    val updatedChapters: Int = 0,
    val skippedChapters: Int = 0,
    val deletedChapters: Int = 0,
    val totalChapters: Int = 0,
    val syncVersion: Int = 0
)

/**
 * 将 Work + Chapters 转换为 SyncPayload
 * 用于导出到 Reading APP
 */
fun exportForSync(work: Work, chapters: List<Chapter>, volumes: Map<String, Volume>): SyncPayload {
    // 按 globalOrder 排序确保章节顺序正确
    val sortedChapters = chapters.sortedBy { it.globalOrder }
    
    // 构建卷ID→卷名的映射
    val volumeNameMap = volumes.entries.associate { it.key to it.value.name.ifEmpty { it.value.title } }
    
    val chapterItems = sortedChapters.mapIndexed { index, ch ->
        ChapterSyncItem(
            chapterId = ch.syncId.ifEmpty { UUID.randomUUID().toString() },
            chapterIndex = index,
            title = ch.title,
            content = ch.content,
            contentHash = md5(ch.content),
            volumeName = ch.volumeId?.takeIf { it.isNotEmpty() }?.let { volumeNameMap[it] }
        )
    }
    
    return SyncPayload(
        bookId = work.syncId.ifEmpty { UUID.randomUUID().toString() },
        bookTitle = work.title,
        description = work.description,
        syncVersion = work.syncVersion,
        lastModified = work.updatedAt,
        chapters = chapterItems
    )
}

/** 计算 MD5 哈希值 */
private fun md5(input: String): String {
    return if (input.isEmpty()) "" else {
        try {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
