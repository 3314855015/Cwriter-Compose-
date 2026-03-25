package com.cwriter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cwriter.data.model.Foreshadowing
import com.cwriter.data.model.ForeshadowingStatus
import com.cwriter.data.model.ParagraphForeshadowing

/**
 * 段落边界信息
 */
data class ParagraphBounds(
    val index: Int,
    val top: Float,      // 相对于内容区域顶部的位置
    val bottom: Float    // 段落底部位置
)

/**
 * 绝对定位伏笔面板
 * 图标根据段落实际位置绝对定位
 * 使用 Box 实现绝对定位，offset 是相对于 Box 左上角的偏移
 */
@Composable
fun ForeshadowingOverlayPanel(
    foreshadowings: List<Foreshadowing>,
    paragraphBounds: Map<Int, ParagraphBounds>,
    currentChapterId: String,
    isClickable: Boolean,
    onIconClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    
    // 计算每个段落的伏笔统计
    // pendingCount: 在该段落创建且未回收的伏笔数
    // recycledCount: 在该段落创建且已回收的伏笔数
    // recycledHereCount: 在该段落回收的伏笔数（不论创建位置）
    val foreshadowingMap = remember(foreshadowings, paragraphBounds.size, currentChapterId) {
        val map = mutableMapOf<Int, ParagraphForeshadowing>()
        paragraphBounds.keys.forEach { index ->
            // 筛选在当前章节该段落创建的伏笔
            val createdHere = foreshadowings.filter {
                it.createdParagraphIndex == index && it.chapterId == currentChapterId
            }

            // 筛选在当前章节该段落回收的伏笔（不论创建位置）
            val recycledHere = foreshadowings.filter {
                it.status == ForeshadowingStatus.RECYCLED &&
                it.recycledChapterId == currentChapterId &&
                it.recycledParagraphIndex == index
            }
            
            map[index] = ParagraphForeshadowing(
                paragraphIndex = index,
                totalCount = createdHere.size + recycledHere.size,
                pendingCount = createdHere.count { it.status == ForeshadowingStatus.PENDING },
                recycledCount = createdHere.count { it.status == ForeshadowingStatus.RECYCLED },
                recycledHereCount = recycledHere.size
            )
        }
        map
    }
    
    // 图标列：绝对定位，宽度由内容决定
    Box(modifier = modifier) {
        // 为每个段落绝对定位图标，位置 = 段落中心Y（对应 UniApp 的 bounds.centerY）
        paragraphBounds.values.sortedBy { it.index }.forEach { bounds ->
            val foreshadowing = foreshadowingMap[bounds.index]
            val centerYDp = ((bounds.top + bounds.bottom) / 2f).dp

            Box(
                modifier = Modifier
                    .offset(y = centerYDp - 12.dp)  // -12dp 使图标垂直居中（图标高24dp的一半）
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ForeshadowingIcon(
                    foreshadowing = foreshadowing,
                    iconColor     = onBackgroundColor,
                    isClickable   = isClickable,
                    onClick       = { onIconClick(bounds.index) }
                )
            }
        }
    }
}

/**
 * 伏笔图标
 * 颜色规则：
 * - 灰色半透明 + "+"：无伏笔
 * - 浅灰色 + 数字：只有未回收的伏笔（在该段创建）
 * - 浅蓝色 + 数字：只有已回收的伏笔（在该段创建或回收）
 * - 深蓝色 + 数字：既有未回收也有已回收
 */
@Composable
private fun ForeshadowingIcon(
    foreshadowing: ParagraphForeshadowing?,
    iconColor: Color,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    // 根据状态决定颜色
    val hasPending = (foreshadowing?.pendingCount ?: 0) > 0
    val hasRecycled = (foreshadowing?.recycledCount ?: 0) > 0 || (foreshadowing?.recycledHereCount ?: 0) > 0
    
    val (backgroundColor, textColor, number) = when {
        foreshadowing == null || (!hasPending && !hasRecycled) -> {
            Triple(
                iconColor.copy(alpha = 0.1f),
                iconColor.copy(alpha = 0.5f),
                null
            )
        }
        hasPending && hasRecycled -> {
            // 深蓝色：既有创建又有回收
            Triple(
                iconColor.copy(alpha = 0.8f),
                MaterialTheme.colorScheme.background,
                (foreshadowing.pendingCount + foreshadowing.recycledCount + foreshadowing.recycledHereCount)
            )
        }
        hasPending -> {
            // 浅灰色：只有未回收
            Triple(
                iconColor.copy(alpha = 0.3f),
                iconColor,
                foreshadowing.pendingCount
            )
        }
        else -> {
            // 浅蓝色：只有已回收（创建时已回收 或 在此段回收）
            Triple(
                iconColor.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.background,
                maxOf(foreshadowing.recycledCount, foreshadowing.recycledHereCount)
            )
        }
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (isClickable) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = number?.toString() ?: "+",
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            color      = textColor
        )
    }
}
