package com.cwriter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.cwriter.ui.theme.LocalIsDark

private val Orange = Color(0xFFFF6B35)

@Composable
fun RenameVolumeDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val isDark = LocalIsDark.current
    var volumeName by remember { mutableStateOf(currentName) }
    val maxLen = 30
    val focusRequester = remember { FocusRequester() }

    val bgModal  = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val bgInput  = if (isDark) Color(0xFF383838) else Color(0xFFF5F5F5)
    val textMain = if (isDark) Color(0xFFFFFFFF) else Color(0xFF333333)
    val textHint = if (isDark) Color(0xFF808080) else Color(0xFFB3B3B3)
    val textSub  = if (isDark) Color(0xFFB3B3B3) else Color(0xFF666666)
    val divider  = if (isDark) Color(0xFF404040) else Color(0xFFEEEEEE)

    val canConfirm = volumeName.isNotBlank() && volumeName != currentName

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                        text = "修改卷名称",
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
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "卷名称",
                        fontSize = 13.sp,
                        color = textSub,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgInput)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = volumeName,
                            onValueChange = { if (it.length <= maxLen) volumeName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(fontSize = 15.sp, color = textMain),
                            cursorBrush = SolidColor(Orange),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (volumeName.isEmpty()) {
                                    Text("请输入新的卷名称", fontSize = 15.sp, color = textHint)
                                }
                                inner()
                            }
                        )
                    }
                    Text(
                        text = "${volumeName.length}/$maxLen",
                        fontSize = 12.sp,
                        color = textHint,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canConfirm) Orange else Orange.copy(alpha = 0.4f))
                        .clickable(enabled = canConfirm) { onConfirm(volumeName.trim()) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("确认", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}
