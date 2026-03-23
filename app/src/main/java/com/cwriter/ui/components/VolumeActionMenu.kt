package com.cwriter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cwriter.ui.theme.LocalIsDark

private val Red = Color(0xFFFF4444)

/**
 * 卷操作菜单 — 直接作为 overlay 渲染在父 Box 里，不用 Dialog
 * 避免 Dialog Window 层级带来的手势竞争问题
 */
@Composable
fun VolumeActionMenu(
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark   = LocalIsDark.current
    val bgMenu   = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val textMain = if (isDark) Color(0xFFFFFFFF) else Color(0xFF333333)
    val textSub  = if (isDark) Color(0xFFB3B3B3) else Color(0xFF666666)
    val divider  = if (isDark) Color(0xFF404040) else Color(0xFFEEEEEE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(bgMenu)
                .navigationBarsPadding()
                .pointerInput(Unit) { detectTapGestures { } } // 拦截，防止点穿到遮罩
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss(); onRename() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("修改卷名", fontSize = 16.sp, color = textMain)
            }

            HorizontalDivider(color = divider, thickness = 0.5.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss(); onDelete() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("删除卷", fontSize = 16.sp, color = Red, fontWeight = FontWeight.Medium)
            }

            HorizontalDivider(color = divider, thickness = 4.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("取消", fontSize = 16.sp, color = textSub)
            }
        }
    }
}

/**
 * 章节操作菜单 — 同上，overlay 方式
 */
@Composable
fun ChapterActionMenu(
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark  = LocalIsDark.current
    val bgMenu  = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val textSub = if (isDark) Color(0xFFB3B3B3) else Color(0xFF666666)
    val divider = if (isDark) Color(0xFF404040) else Color(0xFFEEEEEE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(bgMenu)
                .navigationBarsPadding()
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss(); onDelete() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("删除章节", fontSize = 16.sp, color = Red, fontWeight = FontWeight.Medium)
            }

            HorizontalDivider(color = divider, thickness = 4.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("取消", fontSize = 16.sp, color = textSub)
            }
        }
    }
}
