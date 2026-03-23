package com.cwriter.ui.theme

import androidx.compose.runtime.compositionLocalOf

/** 全局主题状态，任意层级的 Composable 都可以通过 LocalIsDark.current 读取 */
val LocalIsDark = compositionLocalOf { false }
