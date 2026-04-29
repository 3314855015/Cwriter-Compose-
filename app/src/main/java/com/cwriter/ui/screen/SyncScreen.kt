package com.cwriter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.Work
import com.cwriter.data.repository.FileStorageRepository
import com.cwriter.ui.theme.LocalIsDark

/**
 * 同步到阅读 APP 页面
 * 功能：
 * 1. 显示当前作品的同步状态（syncId、syncVersion、章节数）
 * 2. 检测 Reading APP 是否已安装
 * 3. 执行同步操作（导出 JSON → 通过 Intent 发送）
 */

// ====== 主题色 ======
private val BgPageLight   = Color(0xFFF5F5F5)
private val BgPageDark    = Color(0xFF1A1A1A)
private val BgCardLight   = Color(0xFFFFFFFF)
private val BgCardDark    = Color(0xFF2D2D2D)
private val TextMainLight = Color(0xFF333333)
private val TextMainDark  = Color(0xFFFFFFFF)
private val TextSubLight  = Color(0xFF666666)
private val TextSubDark   = Color(0xFFB3B3B3)
private val Orange        = Color(0xFFFF6B35)
private val Green         = Color(0xFF34C759)
private val BorderLight   = Color(0xFFE0E0E0)
private val BorderDark    = Color(0xFF404040)

@Composable
fun SyncScreen(
    userId: String,
    workId: String,
    onNavigateBack: () -> Unit,
    viewModel: SyncViewModel = viewModel()
) {
    val isDark = LocalIsDark.current
    val context = LocalContext.current
    
    // 动态颜色
    val bgPage   = if (isDark) BgPageDark  else BgPageLight
    val bgCard   = if (isDark) BgCardDark  else BgCardLight
    val textMain = if (isDark) TextMainDark else TextMainLight
    val textSub  = if (isDark) TextSubDark  else TextSubLight
    val border   = if (isDark) BorderDark  else BorderLight
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(userId, workId) {
        viewModel.init(context, userId, workId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgPage)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶栏
            SyncTopBar(
                title = "同步到阅读",
                textMain = textMain,
                textSub = textSub,
                onBack = onNavigateBack
            )
            
            Spacer(Modifier.height(16.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Orange)
                }
            } else {
                // 作品信息卡片
                WorkInfoCard(
                    work = uiState.work,
                    totalChapters = uiState.totalChapters,
                    lastSyncTime = uiState.lastSyncTime,
                    bgCard = bgCard,
                    textMain = textMain,
                    textSub = textSub,
                    border = border
                )
                
                Spacer(Modifier.height(12.dp))
                
                // 同步状态卡片
                SyncStatusCard(
                    hasSyncedBefore = uiState.hasSyncedBefore,
                    syncVersion = uiState.syncVersion,
                    bgCard = bgCard,
                    textMain = textMain,
                    textSub = textSub,
                    border = border
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Reading APP 状态（乐观策略：默认就绪）
                ReadingAppStatusCard(
                    status = uiState.readingAppStatus,
                    bgCard = bgCard,
                    textMain = textMain,
                    textSub = textSub,
                    green = Green,
                    border = border
                )
                
                Spacer(Modifier.height(24.dp))
                
                // 主按钮（不再因"未检测到"而禁用）
                SyncButton(
                    isSyncing = uiState.isSyncing,
                    orange = Orange,
                    onClick = { viewModel.syncToReadingApp(context) }
                )
                
                // 进度/结果区域
                if (uiState.isSyncing || uiState.syncMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SyncResultPanel(
                        isSyncing = uiState.isSyncing,
                        message = uiState.syncMessage,
                        isError = uiState.isError,
                        orange = Orange,
                        green = Green,
                        bgCard = bgCard,
                        textMain = textMain,
                        textSub = textSub,
                        border = border
                    )
                }
                
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ─── 顶栏 ────────────────────────────────────────
@Composable
private fun SyncTopBar(
    title: String,
    textMain: Color,
    textSub: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (LocalIsDark.current) Color(0xFF2D2D2D) else Color.White)
            .padding(start = 8.dp, end = 12.dp, top = 32.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("← 返回", fontSize = 15.sp, color = textSub, fontWeight = FontWeight.Medium)
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = textMain,
            textAlign = TextAlign.Center
        )
        // 右侧占位，保持标题居中
        Spacer(Modifier.width(60.dp))
    }
    HorizontalDivider(
        color = if (LocalIsDark.current) BorderDark else BorderLight,
        thickness = 0.5.dp
    )
}

// ─── 作品信息卡片 ──────────────────────────────────
@Composable
private fun WorkInfoCard(
    work: Work,
    totalChapters: Int,
    lastSyncTime: String?,
    bgCard: Color,
    textMain: Color,
    textSub: Color,
    border: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("作品信息", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textMain)
            Spacer(Modifier.height(12.dp))
            
            InfoRow("书名", work.title.ifEmpty { "未命名" }, textMain, textSub)
            InfoRow("同步ID", work.syncId.ifEmpty { "未生成" }, textMain, textSub)
            InfoRow("章节总数", "$totalChapters 章", textMain, textSub)
            InfoRow("同步版本", "v${work.syncVersion}", textMain, textSub)
            
            if (lastSyncTime != null) {
                InfoRow("上次同步", lastSyncTime, textMain, textSub)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, textMain: Color, textSub: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = textSub)
        Text(value, fontSize = 13.sp, color = textMain, maxLines = 1)
    }
}

// ─── 同步状态卡片 ─────────────────────────────────
@Composable
private fun SyncStatusCard(
    hasSyncedBefore: Boolean,
    syncVersion: Int,
    bgCard: Color,
    textMain: Color,
    textSub: Color,
    border: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("同步状态", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textMain)
            Spacer(Modifier.height(12.dp))
            
            when {
                !hasSyncedBefore -> {
                    Text(
                        "尚未同步过此作品，首次同步将导出全部内容",
                        fontSize = 13.sp, color = textSub
                    )
                }
                else -> {
                    Text(
                        "当前版本 v$syncVersion | 已同步过，再次同步将检测增量变化",
                        fontSize = 13.sp, color = textSub
                    )
                }
            }
        }
    }
}

// ─── Reading APP 状态 ─────────────────────────────
@Composable
private fun ReadingAppStatusCard(
    status: String,
    bgCard: Color,
    textMain: Color,
    textSub: Color,
    green: Color,
    border: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("阅读 APP", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textMain)
            Spacer(Modifier.height(12.dp))
            
            val isReady = (status == "ready")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (isReady) green else Color.Gray, CircleShape)
                )
                Text(
                    when (status) {
                        "detecting" -> "检测中..."
                        "ready" -> "就绪"
                        else -> "未知"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isReady) green else Color.Gray
                )
            }
            
            if (isReady) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "包名：com.reading.my | 点击同步按钮即可发送数据",
                    fontSize = 12.sp, color = textSub
                )
            }
        }
    }
}

// ─── 主按钮 ──────────────────────────────────────
@Composable
private fun SyncButton(
    isSyncing: Boolean,
    orange: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isSyncing,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = orange)
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("正在同步...", fontSize = 15.sp, color = Color.White)
        } else {
            Text("开始同步", fontSize = 15.sp, color = Color.White)
        }
    }
}

// ─── 结果面板 ─────────────────────────────────────
@Composable
private fun SyncResultPanel(
    isSyncing: Boolean,
    message: String,
    isError: Boolean,
    orange: Color,
    green: Color,
    bgCard: Color,
    textMain: Color,
    textSub: Color,
    border: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgCard,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = orange)
                Spacer(Modifier.height(8.dp))
                Text(message, fontSize = 13.sp, color = textSub, textAlign = TextAlign.Center)
            } else {
                val iconColor = if (isError) ErrorRed else green
                Text(message, fontSize = 13.sp, color = if (isError) ErrorRed else textMain, textAlign = TextAlign.Center)
            }
        }
    }
}

private val ErrorRed = Color(0xFFFF3B30)
