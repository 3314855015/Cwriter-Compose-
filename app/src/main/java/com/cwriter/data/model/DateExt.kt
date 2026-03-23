package com.cwriter.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date 扩展函数：转换为 ISO 8601 格式字符串
 * 项目中统一使用此扩展函数，避免重复代码
 */
fun Date.toISOString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(this)
}

/**
 * 获取当前时间的 ISO 8601 格式字符串
 */
fun nowISOString(): String = Date().toISOString()
