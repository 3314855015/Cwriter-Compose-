package com.cwriter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 创建卷对话框
 */
@Composable
fun CreateVolumeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var volumeName by remember { mutableStateOf("") }
    var volumeDescription by remember { mutableStateOf("") }
    
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
                    text = "创建新卷",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 卷名输入
                OutlinedTextField(
                    value = volumeName,
                    onValueChange = { volumeName = it },
                    label = { Text("卷名") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )
                
                // 卷描述输入
                OutlinedTextField(
                    value = volumeDescription,
                    onValueChange = { volumeDescription = it },
                    label = { Text("卷描述（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    minLines = 3,
                    maxLines = 5
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
                            if (volumeName.isNotBlank()) {
                                onConfirm(volumeName, volumeDescription)
                            }
                        },
                        enabled = volumeName.isNotBlank()
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
