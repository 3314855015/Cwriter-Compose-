package com.cwriter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cwriter.data.model.Volume

/**
 * 创建章节对话框
 * 支持选择目标卷（分卷作品）
 */
@Composable
fun CreateChapterDialog(
    volumes: List<Volume>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit // title, content, volumeId
) {
    var chapterTitle by remember { mutableStateOf("") }
    var chapterContent by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedVolume by remember { mutableStateOf<Volume?>(volumes.firstOrNull()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "创建新章节",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 如果有多个卷，显示卷选择器
                if (volumes.size > 1) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedVolume?.title ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("选择卷") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            volumes.forEach { volume ->
                                DropdownMenuItem(
                                    text = { Text(volume.title) },
                                    onClick = {
                                        selectedVolume = volume
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // 章节标题输入
                OutlinedTextField(
                    value = chapterTitle,
                    onValueChange = { chapterTitle = it },
                    label = { Text("章节标题") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )
                
                // 章节内容输入
                OutlinedTextField(
                    value = chapterContent,
                    onValueChange = { chapterContent = it },
                    label = { Text("章节内容（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .height(120.dp),
                    maxLines = 6
                )
                
                // 按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (chapterTitle.isNotBlank() && selectedVolume != null) {
                                onConfirm(
                                    chapterTitle,
                                    chapterContent,
                                    selectedVolume!!.id
                                )
                            }
                        },
                        enabled = chapterTitle.isNotBlank() && selectedVolume != null
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
