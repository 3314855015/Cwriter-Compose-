package com.cwriter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cwriter.data.model.Volume
import com.cwriter.ui.theme.LocalIsDark

private val Orange = Color(0xFFFF6B35)

/** 数字转中文序数，超过10用阿拉伯数字兜底 */
private fun toChineseOrdinal(n: Int): String {
    val map = listOf("零","一","二","三","四","五","六","七","八","九","十",
        "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十")
    return if (n in 1..20) map[n] else n.toString()
}

@Composable
fun CreateChapterDialog(
    volumes: List<Volume>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val isDark = LocalIsDark.current
    var chapterTitle by remember { mutableStateOf("") }
    // 默认选最后一个卷（序号最大）
    var selectedVolume by remember { mutableStateOf(volumes.lastOrNull()) }
    var showVolumeMenu by remember { mutableStateOf(false) }
    val maxLen = 50
    val focusRequester = remember { FocusRequester() }

    val bgModal  = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val bgInput  = if (isDark) Color(0xFF383838) else Color(0xFFF5F5F5)
    val bgMenu   = if (isDark) Color(0xFF333333) else Color(0xFFFFFFFF)
    val textMain = if (isDark) Color(0xFFFFFFFF) else Color(0xFF333333)
    val textHint = if (isDark) Color(0xFF808080) else Color(0xFFB3B3B3)
    val textSub  = if (isDark) Color(0xFFB3B3B3) else Color(0xFF666666)
    val divider  = if (isDark) Color(0xFF404040) else Color(0xFFEEEEEE)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val canConfirm = chapterTitle.isNotBlank() && selectedVolume != null

    // 卷选择菜单 — 独立 Dialog，底部弹出
    if (showVolumeMenu) {
        Dialog(
            onDismissRequest = { showVolumeMenu = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showVolumeMenu = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(bgMenu)
                        .navigationBarsPadding()
                        .clickable(enabled = false) {}
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "选择目标卷",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMain,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    HorizontalDivider(color = divider, thickness = 0.5.dp)

                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        itemsIndexed(volumes) { index, volume ->
                            val isSelected = volume.id == selectedVolume?.id
                            val label = "第${toChineseOrdinal(index + 1)}卷 ${volume.name.ifEmpty { volume.title.ifEmpty { "未命名卷" } }}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedVolume = volume
                                        showVolumeMenu = false
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, fontSize = 15.sp,
                                    color = if (isSelected) Orange else textMain)
                                if (isSelected) {
                                    Text("✓", fontSize = 15.sp, color = Orange, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (index < volumes.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = divider, thickness = 0.5.dp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = divider, thickness = 4.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVolumeMenu = false }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("取消", fontSize = 15.sp, color = textSub)
                    }
                }
            }
        }
    }

    // 主面板 — 居中显示
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgModal)
            ) {
                // 标题栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "取消",
                        fontSize = 15.sp,
                        color = textSub,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clickable(onClick = onDismiss)
                    )
                    Text(
                        text = "创建新章节",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                HorizontalDivider(color = divider, thickness = 0.5.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 卷选择器
                    Column {
                        Text("目标卷", fontSize = 13.sp, color = textSub,
                            modifier = Modifier.padding(bottom = 10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgInput)
                                .clickable { showVolumeMenu = true }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedIndex = volumes.indexOfFirst { it.id == selectedVolume?.id }
                            Text(
                                text = selectedVolume?.let { vol ->
                                    val idx = if (selectedIndex >= 0) selectedIndex else volumes.lastIndex
                                    "第${toChineseOrdinal(idx + 1)}卷 ${vol.name.ifEmpty { vol.title.ifEmpty { "未命名卷" } }}"
                                } ?: "请选择卷",
                                fontSize = 15.sp,
                                color = if (selectedVolume != null) textMain else textHint
                            )
                            Text("▼", fontSize = 11.sp, color = textHint)
                        }
                    }

                    // 章节标题输入
                    Column {
                        Text("章节标题", fontSize = 13.sp, color = textSub,
                            modifier = Modifier.padding(bottom = 10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgInput)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = chapterTitle,
                                onValueChange = { if (it.length <= maxLen) chapterTitle = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(fontSize = 15.sp, color = textMain),
                                cursorBrush = SolidColor(Orange),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (chapterTitle.isEmpty()) {
                                        Text("请输入章节标题", fontSize = 15.sp, color = textHint)
                                    }
                                    inner()
                                }
                            )
                        }
                        Text(
                            text = "${chapterTitle.length}/$maxLen",
                            fontSize = 12.sp,
                            color = textHint,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 6.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canConfirm) Orange else Orange.copy(alpha = 0.4f))
                        .clickable(enabled = canConfirm) {
                            onConfirm(chapterTitle.trim(), "", selectedVolume!!.id)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("创建", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}
