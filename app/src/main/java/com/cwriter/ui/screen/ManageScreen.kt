package com.cwriter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cwriter.ui.theme.AccentOrange

// 管理页颜色（亮色主题）
private val ManageBg = Color(0xFFF5F5F5)
private val ManageCardBg = Color(0xFFFFFFFF)
private val ManageTextPrimary = Color(0xFF333333)
private val ManageTextSecondary = Color(0xFF666666)
private val ManageTextHint = Color(0xFFAAAAAA)
private val ManageBorder = Color(0xFFEEEEEE)
private val ManageOrangeLight = Color(0xFFFFF0E8)
private val ManageOrangeBorder = AccentOrange

// 样式配置数据
private data class StyleRow(val label: String, var fontName: String, var size: Int, var bold: Boolean)

@Composable
fun ManageScreen() {
    var currentTab by remember { mutableStateOf(0) } // 0=导入, 1=导出

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ManageBg)
    ) {
        // 顶部两个大 Tab 卡片
        ManageTabBar(
            selectedTab = currentTab,
            onTabSelected = { currentTab = it }
        )

        // 内容区
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                0 -> ImportPanel()
                1 -> ExportPanel()
            }
        }
    }
}

// ===== 顶部 Tab 卡片 =====
@Composable
private fun ManageTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ManageCardBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("导入小说" to Icons.Default.Download, "导出小说" to Icons.Default.Upload)
            .forEachIndexed { index, (label, icon) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ManageOrangeLight else ManageCardBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ManageOrangeBorder else ManageBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) AccentOrange else ManageTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) AccentOrange else ManageTextSecondary
                        )
                    }
                }
            }
    }
}

// ===== 样式选择区（导入/导出共用）=====
@Composable
private fun StyleSection() {
    val styles = remember {
        mutableStateListOf(
            StyleRow("书名", "宋体", 22, true),
            StyleRow("卷名", "宋体", 18, true),
            StyleRow("章名", "宋体", 16, true),
            StyleRow("正文", "宋体", 14, false),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ManageCardBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "样式选择",
            fontSize = 13.sp,
            color = AccentOrange,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        styles.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 标签
                Text(
                    text = row.label,
                    fontSize = 14.sp,
                    color = ManageTextPrimary,
                    modifier = Modifier.width(28.dp)
                )
                // 字体选择框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .border(1.dp, ManageBorder, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(ManageCardBg)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = row.fontName, fontSize = 13.sp, color = ManageTextPrimary)
                }
                // 字号框
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(34.dp)
                        .border(1.dp, ManageBorder, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(ManageCardBg)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = row.size.toString(), fontSize = 13.sp, color = ManageTextPrimary)
                }
                // B 加粗按钮
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (row.bold) AccentOrange else ManageBorder)
                        .clickable { styles[index] = row.copy(bold = !row.bold) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (row.bold) Color.White else ManageTextSecondary
                    )
                }
            }
        }
    }
}

// ===== 功能开关区 =====
@Composable
private fun FunctionSwitchSection(
    exportFormat: Int,          // 0=PDF, 1=Word
    onFormatChange: (Int) -> Unit,
    showSummary: Boolean,
    onSummaryChange: (Boolean) -> Unit,
    hasVolume: Boolean,
    onVolumeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ManageCardBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "功能开关",
            fontSize = 13.sp,
            color = AccentOrange,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 格式
        SwitchRow(
            label = "格式",
            options = listOf("PDF", "Word"),
            selectedIndex = exportFormat,
            onSelect = onFormatChange
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 简介
        SwitchRow(
            label = "简介",
            options = listOf("有", "无"),
            selectedIndex = if (showSummary) 0 else 1,
            onSelect = { onSummaryChange(it == 0) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 分卷
        SwitchRow(
            label = "分卷",
            options = listOf("是", "否"),
            selectedIndex = if (hasVolume) 0 else 1,
            onSelect = { onVolumeChange(it == 0) }
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = ManageTextPrimary,
            modifier = Modifier.width(36.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) ManageOrangeLight else ManageBg)
                        .border(
                            1.dp,
                            if (isSelected) AccentOrange else ManageBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        color = if (isSelected) AccentOrange else ManageTextSecondary
                    )
                }
            }
        }
    }
}

// ===== 导入面板 =====
@Composable
private fun ImportPanel() {
    var exportFormat by remember { mutableIntStateOf(0) }
    var showSummary by remember { mutableStateOf(true) }
    var hasVolume by remember { mutableStateOf(true) }
    var selectedFile by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 样式选择 + 功能开关（横向两列）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 左：样式选择
            Box(modifier = Modifier.weight(1f)) {
                StyleSection()
            }
            // 右：功能开关
            Box(modifier = Modifier.weight(1f)) {
                FunctionSwitchSection(
                    exportFormat = exportFormat,
                    onFormatChange = { exportFormat = it },
                    showSummary = showSummary,
                    onSummaryChange = { showSummary = it },
                    hasVolume = hasVolume,
                    onVolumeChange = { hasVolume = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 文件选择入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ManageOrangeLight)
                .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable { /* TODO: 文件选择器 */ }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = AccentOrange.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = selectedFile ?: "点击选择要导入的文件",
                    fontSize = 14.sp,
                    color = AccentOrange
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 空状态提示
        if (selectedFile == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "请选择要导入的文件", fontSize = 15.sp, color = ManageTextHint)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "支持 DOCX 格式", fontSize = 13.sp, color = Color(0xFFCCCCCC))
            }
        }
    }
}

// ===== 导出面板 =====
@Composable
private fun ExportPanel() {
    var exportFormat by remember { mutableIntStateOf(0) }
    var showSummary by remember { mutableStateOf(true) }
    var hasVolume by remember { mutableStateOf(true) }
    var selectedWork by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 72.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 样式选择 + 功能开关
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { StyleSection() }
                Box(modifier = Modifier.weight(1f)) {
                    FunctionSwitchSection(
                        exportFormat = exportFormat,
                        onFormatChange = { exportFormat = it },
                        showSummary = showSummary,
                        onSummaryChange = { showSummary = it },
                        hasVolume = hasVolume,
                        onVolumeChange = { hasVolume = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 选择作品
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ManageCardBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "选择作品", fontSize = 13.sp, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                    if (selectedWork != null) {
                        Text(text = selectedWork!!, fontSize = 12.sp, color = ManageTextHint, maxLines = 1)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ManageOrangeLight)
                        .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { /* TODO: 选择作品 */ }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedWork ?: "请选择要导出的作品",
                        fontSize = 14.sp,
                        color = AccentOrange
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 导出路径
            if (selectedWork != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ManageCardBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(text = "导出路径", fontSize = 13.sp, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "_downloads/$selectedWork.${if (exportFormat == 0) "pdf" else "docx"}",
                            fontSize = 13.sp,
                            color = ManageTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // 底部固定按钮：预览效果
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFFEEEEEE).copy(alpha = 0.95f))
                .clickable { /* TODO: 预览 */ }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "预览效果",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ManageTextPrimary
            )
        }
    }
}
