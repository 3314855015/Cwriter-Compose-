package com.cwriter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cwriter.data.model.Volume
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Work
import com.cwriter.data.repository.FileStorageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 分卷作品 ViewModel - 从 Vue3 代码移植
 * 管理卷和章节的列表、状态、操作
 */
class VolumedWorkViewModel : ViewModel() {
    
    private var repository: FileStorageRepository? = null
    private var userId: String = "default_user"
    private var workId: String = ""
    
    // 作品信息
    private val _workInfo = MutableStateFlow(Work())
    val workInfo: StateFlow<Work> = _workInfo.asStateFlow()
    
    // 卷列表
    private val _volumes = MutableStateFlow<List<Volume>>(emptyList())
    val volumes: StateFlow<List<Volume>> = _volumes.asStateFlow()
    
    // 章节按卷分组
    private val _chaptersByVolume = MutableStateFlow<Map<String, List<Chapter>>>(emptyMap())
    val chaptersByVolume: StateFlow<Map<String, List<Chapter>>> = _chaptersByVolume.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 排序顺序（asc: 正序, desc: 倒序）
    private val _sortOrder = MutableStateFlow("asc")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()
    
    // 当前展开的卷ID（互斥锁：只允许一个卷展开）
    private val _expandedVolumeId = MutableStateFlow<String?>(null)
    val expandedVolumeId: StateFlow<String?> = _expandedVolumeId.asStateFlow()
    
    // 已加载的卷ID集合（懒加载）
    private val loadedVolumeIds = mutableSetOf<String>()
    
    // 事件通道
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()
    
    sealed class Event {
        data class ShowToast(val message: String, val isError: Boolean = false) : Event()
        data class NavigateToEditor(val workId: String, val chapterId: String, val volumeId: String) : Event()
    }
    
    fun init(context: Context, uid: String, wid: String) {
        repository = FileStorageRepository(context)
        userId = uid
        workId = wid
        loadData()
    }
    
    /**
     * 加载数据
     * 对应 Vue 代码中的 loadData 方法
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 读取作品配置
                val work = repository?.getWork(userId, workId)
                if (work != null) {
                    _workInfo.value = work
                }
                
                // 根据作品类型加载数据
                if (work?.structureType == Work.StructureType.VOLUMED) {
                    // 分卷作品 - 加载卷和章节
                    loadVolumesAndChapters()
                } else {
                    // 不分卷作品 - 显示提示（不再自动迁移）
                    _events.emit(Event.ShowToast("该作品不是分卷结构", false))
                    _isLoading.value = false
                    return@launch
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _events.emit(Event.ShowToast("加载数据失败: ${e.message}", true))
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载卷和章节
     * 对应 Vue 代码中的 loadVolumesAndChapters 方法
     */
    private suspend fun loadVolumesAndChapters() {
        try {
            val volumeList = repository?.getVolumes(userId, workId) ?: emptyList()
            _volumes.value = volumeList
            
            // 懒加载：初始只加载第一个卷的章节数据
            if (volumeList.isNotEmpty()) {
                val firstVolumeId = volumeList[0].id
                loadVolumeChapters(firstVolumeId)
                
                // 默认展开第一个卷
                _expandedVolumeId.value = firstVolumeId
            }
        } catch (e: Exception) {
            _events.emit(Event.ShowToast("加载卷章节失败: ${e.message}", true))
        }
    }
    
    /**
     * 转换为卷结构（兼容旧数据）- 已取消
     * 对应 Vue 代码中的 convertToVolumeStructure 方法
     * 
     * 注意：此功能已禁用，不再自动将非分卷作品转换为分卷结构
     */
    /*
    private suspend fun convertToVolumeStructure() {
        try {
            // 读取旧的章节数据
            val oldChapters = repository?.getChapters(userId, workId) ?: emptyList()
            
            // 创建默认卷
            val defaultVolume = repository?.createVolume(userId, workId, Volume(
                name = "正文",
                description = "默认卷"
            ))
            
            if (defaultVolume != null) {
                // 迁移章节到默认卷
                for (chapter in oldChapters) {
                    repository?.createChapter(userId, workId, defaultVolume.id, chapter)
                }
                
                // 更新作品配置为分卷结构
                val work = repository?.getWork(userId, workId)
                if (work != null) {
                    work.structureType = Work.StructureType.VOLUMED
                    repository?.updateWork(userId, work)
                }
                
                // 重新加载
                loadVolumesAndChapters()
            }
        } catch (e: Exception) {
            _events.emit(Event.ShowToast("转换卷结构失败: ${e.message}", true))
        }
    }
    */
    
    /**
     * 切换排序顺序
     * 对应 Vue 代码中的 toggleSortOrder 方法
     */
    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == "asc") "desc" else "asc"
    }
    
    /**
     * 切换卷展开（互斥锁 + 懒加载）
     * 对应 Vue 代码中的 toggleVolumeExpand 方法
     */
    fun toggleVolumeExpand(volumeId: String) {
        // 如果点击的是当前展开的卷，则收起
        if (_expandedVolumeId.value == volumeId) {
            _expandedVolumeId.value = null
            return
        }
        
        // 否则展开新卷（自动关闭之前的卷）
        _expandedVolumeId.value = volumeId
        
        // 懒加载：如果该卷还未加载章节数据，则加载
        if (!loadedVolumeIds.contains(volumeId)) {
            viewModelScope.launch {
                loadVolumeChapters(volumeId)
            }
        }
    }
    
    /**
     * 加载单个卷的章节数据
     * 对应 Vue 代码中的 loadVolumeChapters 方法
     */
    suspend fun loadVolumeChapters(volumeId: String) {
        try {
            val chapters = repository?.getChaptersByVolume(userId, workId, volumeId) ?: emptyList()
            
            // 为每个章节添加 volume_id
            val chaptersWithVolumeId = chapters.map { chapter ->
                chapter.copy(volumeId = volumeId)
            }
            
            // 更新章节数据
            val currentMap = _chaptersByVolume.value.toMutableMap()
            currentMap[volumeId] = chaptersWithVolumeId
            _chaptersByVolume.value = currentMap
            
            // 标记为已加载
            loadedVolumeIds.add(volumeId)
        } catch (e: Exception) {
            _events.emit(Event.ShowToast("加载卷章节失败: ${e.message}", true))
            
            // 设置为空列表
            val currentMap = _chaptersByVolume.value.toMutableMap()
            currentMap[volumeId] = emptyList()
            _chaptersByVolume.value = currentMap
        }
    }
    
    /**
     * 创建卷
     * 对应 Vue 代码中的 createVolume 方法
     */
    fun createVolume(name: String, description: String) {
        viewModelScope.launch {
            try {
                val volume = Volume(
                    name = name,
                    title = name,
                    description = description
                )
                val newVolume = repository?.createVolume(userId, workId, volume)

                if (newVolume != null) {
                    // 重新加载卷列表
                    val updatedVolumes = repository?.getVolumes(userId, workId) ?: emptyList()
                    _volumes.value = updatedVolumes

                    // 新卷标记为已加载（空章节），避免展开时出现 loading
                    val currentMap = _chaptersByVolume.value.toMutableMap()
                    currentMap[newVolume.id] = emptyList()
                    _chaptersByVolume.value = currentMap
                    loadedVolumeIds.add(newVolume.id)

                    // 自动展开新创建的卷
                    _expandedVolumeId.value = newVolume.id

                    _events.emit(Event.ShowToast("卷创建成功", false))
                }
            } catch (e: Exception) {
                _events.emit(Event.ShowToast("创建卷失败: ${e.message}", true))
            }
        }
    }
    
    /**
     * 创建章节
     * 对应 Vue 代码中的 createChapter 方法
     */
    fun createChapter(title: String, content: String, volumeId: String) {
        viewModelScope.launch {
            try {
                val chapter = Chapter(
                    title = title,
                    content = content,
                    volumeId = volumeId
                )
                chapter.updateWordCount()
                
                val newChapter = repository?.createChapter(userId, workId, volumeId, chapter)
                
                if (newChapter != null) {
                    // 重新加载该卷的章节
                    loadVolumeChapters(volumeId)
                    
                    _events.emit(Event.ShowToast("章节创建成功", false))
                }
            } catch (e: Exception) {
                _events.emit(Event.ShowToast("创建章节失败: ${e.message}", true))
            }
        }
    }
    
    /**
     * 重命名卷
     * 对应 Vue 代码中的 renameVolume 方法
     */
    fun renameVolume(volumeId: String, newName: String) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "name" to newName,
                    "title" to newName
                )
                
                repository?.updateVolume(userId, workId, volumeId, updates)
                
                // 重新加载卷列表
                val updatedVolumes = repository?.getVolumes(userId, workId) ?: emptyList()
                _volumes.value = updatedVolumes
                
                _events.emit(Event.ShowToast("卷名修改成功", false))
            } catch (e: Exception) {
                _events.emit(Event.ShowToast("修改卷名失败: ${e.message}", true))
            }
        }
    }
    
    /**
     * 确认删除卷
     * 对应 Vue 代码中的 confirmDeleteVolume 方法
     * 用户确认后，删除卷内所有章节，然后删除卷
     */
    fun confirmDeleteVolume(volume: Volume) {
        viewModelScope.launch {
            try {
                val chapters = chaptersByVolume.value[volume.id] ?: emptyList()
                
                // 如果卷内有章节，先删除所有章节
                if (chapters.isNotEmpty()) {
                    for (chapter in chapters) {
                        repository?.deleteVolumeChapter(userId, workId, volume.id, chapter.id)
                    }
                    
                    // 从缓存中移除这些章节
                    val currentMap = _chaptersByVolume.value.toMutableMap()
                    currentMap[volume.id] = emptyList()
                    _chaptersByVolume.value = currentMap
                    
                    _events.emit(Event.ShowToast("已删除卷内 ${chapters.size} 个章节", false))
                }
                
                // 删除卷
                repository?.deleteVolume(userId, workId, volume.id)
                
                // 重新加载卷列表
                val updatedVolumes = repository?.getVolumes(userId, workId) ?: emptyList()
                _volumes.value = updatedVolumes
                
                // 如果删除的是当前展开的卷，收起它
                if (_expandedVolumeId.value == volume.id) {
                    _expandedVolumeId.value = null
                }
                
                _events.emit(Event.ShowToast("卷删除成功", false))
            } catch (e: Exception) {
                _events.emit(Event.ShowToast("删除卷失败: ${e.message}", true))
            }
        }
    }
    
    /**
     * 删除章节
     */
    fun deleteChapter(volumeId: String, chapterId: String) {
        viewModelScope.launch {
            try {
                repository?.deleteVolumeChapter(userId, workId, volumeId, chapterId)

                // 更新本地缓存
                val currentMap = _chaptersByVolume.value.toMutableMap()
                currentMap[volumeId] = currentMap[volumeId]?.filter { it.id != chapterId } ?: emptyList()
                _chaptersByVolume.value = currentMap

                _events.emit(Event.ShowToast("章节删除成功", false))
            } catch (e: Exception) {
                _events.emit(Event.ShowToast("删除章节失败: ${e.message}", true))
            }
        }
    }

    /**
     * 打开章节
     * 对应 Vue 代码中的 openChapter 方法
     */
    fun openChapter(chapter: Chapter) {
        viewModelScope.launch {
            _events.emit(Event.NavigateToEditor(workId, chapter.id, chapter.volumeId ?: ""))
        }
    }
    
    /**
     * 从目录打开章节
     * 对应 Vue 代码中的 openChapterFromCatalog 方法
     */
    fun openChapterFromCatalog(chapter: Chapter) {
        openChapter(chapter)
    }
    
    /**
     * 检查卷是否已加载
     */
    fun isVolumeLoaded(volumeId: String): Boolean {
        return loadedVolumeIds.contains(volumeId)
    }

    /**
     * 公开的非挂起版本，供 Composable 的 LaunchedEffect 调用
     */
    fun loadVolumeChaptersAsync(volumeId: String) {
        viewModelScope.launch { loadVolumeChapters(volumeId) }
    }
    
    /**
     * 获取章节在卷内的序号（从1开始）
     * 正序：第1章、第2章...
     * 倒序：显示时反转，但序号仍按原始顺序
     */
    fun getChapterNumber(volumeId: String, displayIndex: Int, sortOrder: String): Int {
        val chapters = chaptersByVolume.value[volumeId] ?: return displayIndex + 1
        return if (sortOrder == "desc") {
            // 倒序显示时，第一个显示的是最后一章，序号应该是 chapters.size
            chapters.size - displayIndex
        } else {
            displayIndex + 1
        }
    }

    /**
     * 判断某章节是否是全局最后一章（用于显示"写作中"标签）
     * 全局最后一章 = 最后一个卷（正序）的最后一章
     */
    fun isLastChapterGlobally(volumeId: String, chapterId: String): Boolean {
        val sortedVolumes = volumes.value.sortedBy { it.order }
        if (sortedVolumes.isEmpty()) return false
        val lastVolume = sortedVolumes.last()
        if (lastVolume.id != volumeId) return false
        val chapters = chaptersByVolume.value[volumeId] ?: return false
        if (chapters.isEmpty()) return false
        return chapters.last().id == chapterId
    }
    
    /**
     * 格式化时间
     * 对应 Vue 代码中的 formatTime 方法
     */
    fun formatTime(timestamp: String): String {
        if (timestamp.isEmpty()) return "未知时间"
        
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = sdf.parse(timestamp) ?: return "未知时间"
            val now = Date()
            
            val diff = now.time - date.time
            
            val minutes = diff / 60000
            val hours = diff / 3600000
            val days = diff / 86400000
            
            when {
                minutes < 60 -> "${minutes}分钟前"
                hours < 24 -> "${hours}小时前"
                days < 7 -> "${days}天前"
                else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            "未知时间"
        }
    }
}
