package com.cwriter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cwriter.data.model.Work
import com.cwriter.ui.theme.AccentOrange

/**
 * 创建作品对话框 - 可复用组件
 * 使用 Dialog 实现模态弹窗，固定深色主题样式
 */
@Composable
fun CreateWorkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Work.StructureType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var structureType by remember { mutableStateOf(Work.StructureType.VOLUMED) } // 默认分卷作品

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2A2A2A)) // 固定深色背景
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "创建新作品",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF888888),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 作品标题
                FormLabel(text = "作品标题", required = true)
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    placeholder = "请输入作品标题",
                    maxLength = 100,
                    currentLength = title.length
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 结构类型
                FormLabel(text = "结构类型", required = true)
                Spacer(modifier = Modifier.height(12.dp))

                // 整体作品选项
                StructureOption(
                    text = "整体作品",
                    isSelected = structureType == Work.StructureType.SINGLE,
                    onClick = { structureType = Work.StructureType.SINGLE }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 分卷作品选项
                StructureOption(
                    text = "分卷作品",
                    isSelected = structureType == Work.StructureType.VOLUMED,
                    onClick = { structureType = Work.StructureType.VOLUMED }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 作品描述
                FormLabel(text = "作品描述", required = false, optional = true)
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextArea(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    placeholder = "简要描述作品内容或设定",
                    maxLength = 300,
                    currentLength = description.length
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3A3A3A))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 15.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }

                    // 创建按钮
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (title.isNotBlank()) AccentOrange else Color(0xFF555555)
                            )
                            .clickable(enabled = title.isNotBlank()) {
                                onConfirm(title, description, structureType)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "创建作品",
                            fontSize = 15.sp,
                            color = if (title.isNotBlank()) Color.White else Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormLabel(text: String, required: Boolean = false, optional: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA)
        )
        if (required) {
            Text(
                text = " *",
                fontSize = 14.sp,
                color = AccentOrange
            )
        }
        if (optional) {
            Text(
                text = "（可选）",
                fontSize = 13.sp,
                color = Color(0xFF777777)
            )
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int,
    currentLength: Int
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF3A3A3A))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White
                ),
                cursorBrush = SolidColor(AccentOrange),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$currentLength/$maxLength",
            fontSize = 11.sp,
            color = Color(0xFF666666),
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun CustomTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int,
    currentLength: Int
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF3A3A3A))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(AccentOrange),
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$currentLength/$maxLength",
            fontSize = 11.sp,
            color = Color(0xFF666666),
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun StructureOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AccentOrange else Color(0xFF3A3A3A)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = if (isSelected) Color.White else Color(0xFFCCCCCC)
        )
    }
}
