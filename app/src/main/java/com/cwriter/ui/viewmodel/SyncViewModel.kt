package com.cwriter.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.SyncPayload
import com.cwriter.data.model.Volume
import com.cwriter.data.model.Work
import com.cwriter.data.model.exportForSync
import com.cwriter.data.repository.FileStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 同步页面 ViewModel
 * 负责加载作品信息、检测 Reading APP、执行同步导出
 */
class SyncViewModel : ViewModel() {
    
    private var repository: FileStorageRepository? = null
    private var userId: String = ""
    private var workId: String = ""
    
    // UI 状态
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()
    
    data class SyncUiState(
        val isLoading: Boolean = true,
        val work: Work = Work(),
        val totalChapters: Int = 0,
        val isReadingInstalled: Boolean = false,
        val hasSyncedBefore: Boolean = false,
        val syncVersion: Int = 0,
        val lastSyncTime: String? = null,
        val isSyncing: Boolean = false,
        val syncMessage: String = "",
        val isError: Boolean = false
    )
    
    /** Reading APP 包名 */
    companion object {
        const val READING_PACKAGE = "com.reading.my"
        const val SYNC_ACTION = "com.reading.app.IMPORT_BOOK"
        const val EXTRA_SYNC_PAYLOAD = "SYNC_PAYLOAD"
        const val EXTRA_SOURCE_APP = "SOURCE_APP"
    }
    
    fun init(context: Context, uid: String, wid: String) {
        repository = FileStorageRepository(context)
        userId = uid
        workId = wid
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 1. 加载作品信息
                val work = repository?.getWork(userId, workId)
                if (work != null) {
                    _uiState.value = _uiState.value.copy(
                        work = work,
                        syncVersion = work.syncVersion,
                        hasSyncedBefore = work.syncVersion > 0
                    )
                }
                
                // 2. 统计总章节数（跨卷合计）
                var totalChapters = 0
                val volumes = repository?.getVolumes(userId, workId) ?: emptyList()
                for (volume in volumes) {
                    val chapters = repository?.getChaptersByVolume(userId, workId, volume.id) ?: emptyList()
                    totalChapters += chapters.size
                }
                _uiState.value = _uiState.value.copy(totalChapters = totalChapters)
                
                // 3. 检测 Reading APP 是否已安装
                val isInstalled = checkReadingAppInstalled(context)
                _uiState.value = _uiState.value.copy(isReadingInstalled = isInstalled)
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    syncMessage = "加载数据失败：${e.message}",
                    isError = true
                )
            }
        }
    }
    
    /**
     * 检测 Reading APP 是否已安装
     */
    private fun checkReadingAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(READING_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            android.util.Log.w("SyncViewModel", "Reading APP 未安装: $READING_PACKAGE", e)
            false
        } catch (e: SecurityException) {
            // MIUI/部分 ROM 会拦截跨应用 package 查询，不能误判为"未安装"
            android.util.Log.w("SyncViewModel", "查询 Reading APP 被 OS 拦截（可能需手动授权），默认视为已安装", e)
            true  // 假设已安装，让后续 Intent 操作来真正检验
        } catch (e: Exception) {
            android.util.Log.e("SyncViewModel", "检测 Reading APP 异常", e)
            false
        }
    }
    
    /**
     * 执行同步：导出 → 发送 Intent 到 Reading APP
     */
    fun syncToReadingApp(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = "正在准备数据...", isError = false)
                
                val work = _uiState.value.work
                
                // 1. 收集所有卷和章节
                val volumes = repository?.getVolumes(userId, workId) ?: emptyList()
                val volumesMap = mutableMapOf<String, Volume>()
                val allChapters = mutableListOf<Chapter>()
                
                for (volume in volumes) {
                    volumesMap[volume.id] = volume
                    val chapters = repository?.getChaptersByVolume(userId, workId, volume.id) ?: emptyList()
                    allChapters.addAll(chapters)
                }
                
                if (allChapters.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncMessage = "当前作品没有章节，无法同步",
                        isError = true
                    )
                    return@launch
                }
                
                // 2. 导出为 SyncPayload JSON
                _uiState.value = _uiState.value.copy(syncMessage = "正在生成同步数据...")
                val payload = exportForSync(work, allChapters, volumesMap)
                val jsonStr = payloadToJson(payload)
                
                // 3. 检查大小（超过 1MB 需要其他方案）
                val jsonSize = jsonStr.toByteArray().size
                if (jsonSize > 1024 * 1024) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncMessage = "数据过大 (${jsonSize / 1024}KB)，暂不支持",
                        isError = true
                    )
                    return@launch
                }
                
                // 4. 更新 syncVersion（每次同步 +1）
                work.syncVersion++
                repository?.updateWork(userId, work)
                _uiState.value = _uiState.value.copy(syncVersion = work.syncVersion)
                
                // 5. 通过 Intent 发送到 Reading
                _uiState.value = _uiState.value.copy(syncMessage = "正在发送到阅读APP...")
                val intent = Intent(SYNC_ACTION).apply {
                    putExtra(EXTRA_SYNC_PAYLOAD, jsonStr)
                    putExtra(EXTRA_SOURCE_APP, "cwriter")
                    `package` = READING_PACKAGE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(intent)
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "已发送！共 ${payload.chapters.size} 章，请切换到阅读APP查看导入进度"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "同步失败：${e.message}",
                    isError = true
                )
            }
        }
    }
    
    /**
     * 将 SyncPayload 转为 JSON 字符串
     */
    private fun payloadToJson(payload: SyncPayload): String {
        return JSONObject().apply {
            put("book_id", payload.bookId)
            put("book_title", payload.bookTitle)
            put("author", payload.author)
            put("description", payload.description)
            put("sync_version", payload.syncVersion)
            put("last_modified", payload.lastModified)
            
            val chaptersArray = JSONArray()
            payload.chapters.forEach { ch ->
                chaptersArray.put(JSONObject().apply {
                    put("chapter_id", ch.chapterId)
                    put("chapter_index", ch.chapterIndex)
                    put("title", ch.title)
                    put("content", ch.content)
                    put("content_hash", ch.contentHash)
                    ch.volumeName?.let { put("volume_name", it) }
                })
            }
            put("chapters", chaptersArray)
        }.toString(2)  // 格式化输出，方便调试
    }
}
