package com.cwriter.data.model

/**
 * 卷数据模型
 * 对应 Vue3 代码中的 volume 对象
 */
data class Volume(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val description: String = "",
    val order: Int = 0,
    var chapterCount: Int = 0,
    var wordCount: Int = 0,
    val createdAt: String = nowISOString(),
    val updatedAt: String = nowISOString()
)
