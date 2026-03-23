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
                    // 不分卷作品 - 自动创建默认卷
                    convertToVolumeStructure()
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
     * 转换为卷结构（兼容旧数据）
     * 对应 Vue 代码中的 convertToVolumeStructure 方法
     */
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
                repository?.updateWork(userId, workId, workInfo.value.copy(
                    structureType = Work.StructureType.VOLUMED
                ))
                
                // 重新加载
                loadVolumesAndChapters()
            }
        } catch (e: Exception) {
            _events.emit(Event.ShowToast("转换卷结构失败: ${e.message}", true))
        }
    }
    
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
     * 确认删除卷
     * 对应 Vue 代码中的 confirmDeleteVolume 方法
     */
    fun confirmDeleteVolume(volume: Volume) {
        viewModelScope.launch {
            val chapterCount = chaptersByVolume.value[volume.id]?.size ?: 0
            val volumeName = volume.name ?: "未命名卷"
            
            _events.emit(Event.ShowToast("删除卷功能待实现", false))
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
     * 获取章节编号（全局连续编号）
     * 对应 Vue 代码中的 getChapterNumber 方法
     */
    fun getChapterNumber(volumeId: String, chapterIndex: Int, sortOrder: String): Int {
        var num = 1
        val currentVolumes = if (sortOrder == "desc") volumes.value.reversed() else volumes.value
        
        for (vol in currentVolumes) {
            if (vol.id == volumeId) {
                // 当前卷，加上章节在卷内的索引
                val chapters = chaptersByVolume.value[volumeId] ?: emptyList()
                val actualIndex = if (sortOrder == "desc") chapters.size - 1 - chapterIndex else chapterIndex
                return num + actualIndex
            }
            // 还没到当前卷，累加前面卷的章节数
            num += (chaptersByVolume.value[vol.id]?.size ?: 0)
        }
        
        return num + chapterIndex
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
