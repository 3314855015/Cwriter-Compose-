package com.cwriter.data.repository

import android.content.Context
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Work
import com.cwriter.data.model.UserStats
import com.cwriter.data.model.nowISOString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * 文件存储仓库 - 负责本地文件的读写操作
 */
class FileStorageRepository(private val context: Context) {

    // 基础存储路径（外部私有目录，MT管理器可见，无需额外权限）
    private val baseDir: File
        get() = File(context.getExternalFilesDir(null), "cwriter")

    // 用户目录
    private fun getUserDir(userId: String): File {
        return File(baseDir, "users/$userId").apply { mkdirs() }
    }

    // 作品目录
    private fun getWorkDir(userId: String, workId: String): File {
        return File(getUserDir(userId), "works/$workId").apply { mkdirs() }
    }

    // 章节目录
    private fun getChaptersDir(userId: String, workId: String): File {
        return File(getWorkDir(userId, workId), "chapters").apply { mkdirs() }
    }

    // 作品配置文件
    private fun getWorkConfigFile(userId: String, workId: String): File {
        return File(getWorkDir(userId, workId), "work.config.json")
    }

    // 章节列表文件
    private fun getChaptersListFile(userId: String, workId: String): File {
        return File(getChaptersDir(userId, workId), "chapters.json")
    }

    // 章节内容文件
    private fun getChapterFile(userId: String, workId: String, chapterId: String): File {
        return File(getChaptersDir(userId, workId), "$chapterId.json")
    }

    // 作品列表文件
    private fun getWorksListFile(userId: String): File {
        return File(getUserDir(userId), "works.json")
    }

    // ============ 作品操作 ============

    /**
     * 获取用户所有作品
     */
    suspend fun getWorks(userId: String): List<Work> = withContext(Dispatchers.IO) {
        val file = getWorksListFile(userId)
        if (!file.exists()) return@withContext emptyList()

        try {
            val json = file.readText()
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                jsonToWork(jsonArray.getJSONObject(i))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取单个作品
     */
    suspend fun getWork(userId: String, workId: String): Work? = withContext(Dispatchers.IO) {
        val file = getWorkConfigFile(userId, workId)
        if (!file.exists()) return@withContext null

        try {
            val json = JSONObject(file.readText())
            jsonToWork(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建作品
     */
    suspend fun createWork(userId: String, work: Work): Boolean = withContext(Dispatchers.IO) {
        try {
            // 创建作品目录
            getWorkDir(userId, work.id).mkdirs()
            getChaptersDir(userId, work.id).mkdirs()

            // 保存作品配置
            val configFile = getWorkConfigFile(userId, work.id)
            configFile.writeText(workToJson(work).toString())

            // 更新作品列表
            val works = getWorks(userId).toMutableList()
            works.add(0, work)
            saveWorksList(userId, works)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 更新作品
     */
    suspend fun updateWork(userId: String, work: Work): Boolean = withContext(Dispatchers.IO) {
        try {
            work.updatedAt = System.currentTimeMillis()
            val configFile = getWorkConfigFile(userId, work.id)
            configFile.writeText(workToJson(work).toString())

            // 更新作品列表
            val works = getWorks(userId).toMutableList()
            val index = works.indexOfFirst { it.id == work.id }
            if (index != -1) {
                works[index] = work
                saveWorksList(userId, works)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除作品
     */
    suspend fun deleteWork(userId: String, workId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 删除作品目录
            val workDir = getWorkDir(userId, workId)
            workDir.deleteRecursively()

            // 更新作品列表
            val works = getWorks(userId).toMutableList()
            works.removeAll { it.id == workId }
            saveWorksList(userId, works)

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun saveWorksList(userId: String, works: List<Work>) {
        val file = getWorksListFile(userId)
        val jsonArray = JSONArray()
        works.forEach { jsonArray.put(workToJson(it)) }
        file.writeText(jsonArray.toString())
    }

    // ============ 章节操作 ============

    /**
     * 获取章节列表
     */
    suspend fun getChapters(userId: String, workId: String): List<Chapter> = withContext(Dispatchers.IO) {
        val file = getChaptersListFile(userId, workId)
        if (!file.exists()) return@withContext emptyList()

        try {
            val json = file.readText()
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                jsonToChapter(jsonArray.getJSONObject(i))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取章节内容
     */
    suspend fun getChapter(userId: String, workId: String, chapterId: String): Chapter? = withContext(Dispatchers.IO) {
        val file = getChapterFile(userId, workId, chapterId)
        if (!file.exists()) return@withContext null

        try {
            val json = JSONObject(file.readText())
            jsonToChapter(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建章节
     */
    suspend fun createChapter(userId: String, workId: String, chapter: Chapter): Boolean = withContext(Dispatchers.IO) {
        try {
            // 保存章节内容文件
            val chapterFile = getChapterFile(userId, workId, chapter.id)
            chapterFile.writeText(chapterToJson(chapter).toString())

            // 更新章节列表
            val chapters = getChapters(userId, workId).toMutableList()
            chapters.add(chapter)
            saveChaptersList(userId, workId, chapters)

            // 更新作品章节数
            updateWorkChapterCount(userId, workId, chapters.size)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 更新章节
     */
    suspend fun updateChapter(userId: String, workId: String, chapter: Chapter): Boolean = withContext(Dispatchers.IO) {
        try {
            chapter.updatedAt = System.currentTimeMillis()
            chapter.updateWordCount()

            // 保存章节内容
            val chapterFile = getChapterFile(userId, workId, chapter.id)
            chapterFile.writeText(chapterToJson(chapter).toString())

            // 更新章节列表
            val chapters = getChapters(userId, workId).toMutableList()
            val index = chapters.indexOfFirst { it.id == chapter.id }
            if (index != -1) {
                chapters[index] = chapter
                saveChaptersList(userId, workId, chapters)
            }

            // 更新作品总字数
            updateWorkWordCount(userId, workId)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除章节
     */
    suspend fun deleteChapter(userId: String, workId: String, chapterId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 删除章节文件
            val chapterFile = getChapterFile(userId, workId, chapterId)
            chapterFile.delete()

            // 更新章节列表
            val chapters = getChapters(userId, workId).toMutableList()
            chapters.removeAll { it.id == chapterId }
            saveChaptersList(userId, workId, chapters)

            // 更新作品章节数
            updateWorkChapterCount(userId, workId, chapters.size)

            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除分卷作品中的章节
     * 对应 Vue 代码中的 deleteChapter 方法（分卷版本）
     */
    suspend fun deleteVolumeChapter(userId: String, workId: String, volumeId: String, chapterId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 删除章节文件
            val chapterFile = getVolumeChapterFile(userId, workId, volumeId, chapterId)
            chapterFile.delete()

            // 更新卷的章节列表
            val chapters = getChaptersByVolume(userId, workId, volumeId).toMutableList()
            chapters.removeAll { it.id == chapterId }
            
            // 保存章节列表
            val chaptersListFile = getVolumeChaptersListFile(userId, workId, volumeId)
            val jsonArray = JSONArray()
            chapters.forEach { chapter ->
                jsonArray.put(chapterToJson(chapter))
            }
            chaptersListFile.writeText(jsonArray.toString())

            // 更新卷配置中的章节数
            val volume = getVolume(userId, workId, volumeId)
            if (volume != null) {
                val updatedVolume = volume.copy(
                    chapterCount = chapters.size,
                    updatedAt = nowISOString()
                )
                val configFile = getVolumeConfigFile(userId, workId, volumeId)
                configFile.writeText(volumeToJson(updatedVolume).toString())
                
                // 更新卷列表索引
                updateVolumeInIndex(userId, workId, volumeId, updatedVolume)
            }

            true
        } catch (e: Exception) {
            throw Exception("删除章节失败: ${e.message}")
        }
    }

    private fun saveChaptersList(userId: String, workId: String, chapters: List<Chapter>) {
        val file = getChaptersListFile(userId, workId)
        val jsonArray = JSONArray()
        chapters.forEach { 
            // 列表中不保存内容，只保存元信息
            val json = chapterToJson(it)
            json.remove("content")
            jsonArray.put(json)
        }
        file.writeText(jsonArray.toString())
    }

    private suspend fun updateWorkChapterCount(userId: String, workId: String, count: Int) {
        val work = getWork(userId, workId) ?: return
        work.chapterCount = count
        val configFile = getWorkConfigFile(userId, workId)
        configFile.writeText(workToJson(work).toString())
    }

    private suspend fun updateWorkWordCount(userId: String, workId: String) {
        val chapters = getChapters(userId, workId)
        val totalWords = chapters.sumOf { it.wordCount.toLong() }
        val work = getWork(userId, workId) ?: return
        work.wordCount = totalWords.toInt()
        work.updatedAt = System.currentTimeMillis()
        val configFile = getWorkConfigFile(userId, workId)
        configFile.writeText(workToJson(work).toString())
    }

    // ============ 统计 ============

    // ============ JSON 转换 ============

    private fun jsonToWork(json: JSONObject): Work {
        // 迁移兼容：旧数据没有 sync_id 时自动生成 UUID
        val rawSyncId = json.optString("sync_id", "")
        val syncId = if (rawSyncId.isEmpty()) UUID.randomUUID().toString() else rawSyncId
        
        return Work(
            id = json.optString("id"),
            title = json.optString("title"),
            description = json.optString("description"),
            category = json.optString("category", "novel"),
            structureType = try {
                Work.StructureType.valueOf(json.optString("structure_type", "VOLUMED").uppercase())
            } catch (e: Exception) { Work.StructureType.VOLUMED },
            wordCount = json.optInt("word_count"),
            chapterCount = json.optInt("chapter_count"),
            isFavorite = json.optBoolean("is_favorite"),
            mapCount = json.optInt("map_count"),
            createdAt = json.optLong("created_at"),
            updatedAt = json.optLong("updated_at"),
            isActive = json.optBoolean("is_active", true),
            syncId = syncId,
            syncVersion = json.optInt("sync_version", 0)
        )
    }

    private fun workToJson(work: Work): JSONObject {
        return JSONObject().apply {
            put("id", work.id)
            put("title", work.title)
            put("description", work.description)
            put("category", work.category)
            put("structure_type", work.structureType.name)
            put("word_count", work.wordCount)
            put("chapter_count", work.chapterCount)
            put("is_favorite", work.isFavorite)
            put("map_count", work.mapCount)
            put("created_at", work.createdAt)
            put("updated_at", work.updatedAt)
            put("is_active", work.isActive)
            // 同步字段
            put("sync_id", work.syncId)
            put("sync_version", work.syncVersion)
        }
    }

    private fun jsonToChapter(json: JSONObject): Chapter {
        // 迁移兼容：旧数据没有 sync_id 时自动生成 UUID
        val rawSyncId = json.optString("sync_id", "")
        val syncId = if (rawSyncId.isEmpty()) UUID.randomUUID().toString() else rawSyncId
        
        return Chapter(
            id = json.optString("id"),
            title = json.optString("title"),
            content = json.optString("content"),
            wordCount = json.optInt("word_count"),
            isCompleted = json.optBoolean("is_completed"),
            createdAt = json.optLong("created_at"),
            updatedAt = json.optLong("updated_at"),
            volumeId = json.optString("volume_id"),
            volumeOrder = json.optInt("volume_order"),
            globalOrder = json.optInt("global_order"),
            syncId = syncId
        )
    }

    private fun chapterToJson(chapter: Chapter): JSONObject {
        return JSONObject().apply {
            put("id", chapter.id)
            put("title", chapter.title)
            put("content", chapter.content)
            put("word_count", chapter.wordCount)
            put("is_completed", chapter.isCompleted)
            put("created_at", chapter.createdAt)
            put("updated_at", chapter.updatedAt)
            put("volume_id", chapter.volumeId)
            put("volume_order", chapter.volumeOrder)
            put("global_order", chapter.globalOrder)
            // 同步字段
            put("sync_id", chapter.syncId)
        }
    }

    // ============ 卷管理操作 ============

    /**
     * 获取卷目录
     */
    private fun getVolumesDir(userId: String, workId: String): File {
        return File(getWorkDir(userId, workId), "volumes").apply { mkdirs() }
    }

    /**
     * 获取卷配置目录
     */
    private fun getVolumeDir(userId: String, workId: String, volumeId: String): File {
        return File(getVolumesDir(userId, workId), volumeId).apply { mkdirs() }
    }

    /**
     * 获取卷配置文件
     */
    private fun getVolumeConfigFile(userId: String, workId: String, volumeId: String): File {
        return File(getVolumeDir(userId, workId, volumeId), "volume.config.json")
    }

    /**
     * 获取卷列表文件
     */
    private fun getVolumesListFile(userId: String, workId: String): File {
        return File(getVolumesDir(userId, workId), "volumes.json")
    }

    /**
     * 获取卷内章节目录
     */
    private fun getVolumeChaptersDir(userId: String, workId: String, volumeId: String): File {
        return File(getVolumeDir(userId, workId, volumeId), "chapters").apply { mkdirs() }
    }

    /**
     * 获取卷内章节列表文件
     */
    private fun getVolumeChaptersListFile(userId: String, workId: String, volumeId: String): File {
        return File(getVolumeChaptersDir(userId, workId, volumeId), "chapters.json")
    }
    
    /**
     * 获取卷内单个章节文件
     */
    private fun getVolumeChapterFile(userId: String, workId: String, volumeId: String, chapterId: String): File {
        return File(getVolumeChaptersDir(userId, workId, volumeId), "${chapterId}.json")
    }

    /**
     * 创建卷
     * 对应 Vue 代码中的 createVolume 方法
     */
    suspend fun createVolume(userId: String, workId: String, volume: com.cwriter.data.model.Volume): com.cwriter.data.model.Volume = withContext(Dispatchers.IO) {
        val volumeId = "volume_${System.currentTimeMillis()}"
        val now = nowISOString()
        
        // 支持 name 和 title 两种字段名
        val volumeName = volume.name.ifEmpty { volume.title }.ifEmpty { "未命名卷" }
        
        val newVolume = volume.copy(
            id = volumeId,
            name = volumeName,
            title = volumeName,
            description = volume.description,
            order = 0, // 将在添加到列表时设置
            createdAt = now,
            updatedAt = now
        )
        
        try {
            // 确保目录存在
            getVolumeDir(userId, workId, volumeId)
            getVolumeChaptersDir(userId, workId, volumeId)
            
            // 创建卷配置文件
            val volumeConfigFile = getVolumeConfigFile(userId, workId, volumeId)
            volumeConfigFile.writeText(volumeToJson(newVolume).toString())
            
            // 创建该卷的章节索引
            val chaptersListFile = getVolumeChaptersListFile(userId, workId, volumeId)
            chaptersListFile.writeText(JSONArray().toString())
            
            // 更新卷列表索引
            val volumesListFile = getVolumesListFile(userId, workId)
            val volumesList = mutableListOf<JSONObject>()
            if (volumesListFile.exists()) {
                try {
                    val jsonArray = JSONArray(volumesListFile.readText())
                    for (i in 0 until jsonArray.length()) {
                        volumesList.add(jsonArray.getJSONObject(i))
                    }
                } catch (e: Exception) {
                    // 保持空列表
                }
            }
            
            // 设置卷的顺序
            val volumeWithOrder = newVolume.copy(order = volumesList.size + 1)
            volumesList.add(volumeToJson(volumeWithOrder))
            
            volumesListFile.writeText(JSONArray(volumesList).toString())
            
            // 更新作品修改时间
            val work = getWork(userId, workId)
            if (work != null) {
                work.updatedAt = System.currentTimeMillis()
                updateWork(userId, work)
            }
            
            return@withContext volumeWithOrder
        } catch (e: Exception) {
            throw Exception("创建卷失败: ${e.message}")
        }
    }

    /**
     * 获取卷列表
     * 对应 Vue 代码中的 getVolumes 方法
     */
    suspend fun getVolumes(userId: String, workId: String): List<com.cwriter.data.model.Volume> = withContext(Dispatchers.IO) {
        val file = getVolumesListFile(userId, workId)
        if (!file.exists()) return@withContext emptyList<com.cwriter.data.model.Volume>()
        
        try {
            val jsonArray = JSONArray(file.readText())
            val volumes = mutableListOf<com.cwriter.data.model.Volume>()
            
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                volumes.add(jsonToVolume(json))
            }
            
            // 按顺序排序
            return@withContext volumes.sortedBy { it.order }
        } catch (e: Exception) {
            return@withContext emptyList<com.cwriter.data.model.Volume>()
        }
    }

    /**
     * 获取单个卷信息
     * 对应 Vue 代码中的 getVolume 方法
     */
    suspend fun getVolume(userId: String, workId: String, volumeId: String): com.cwriter.data.model.Volume? = withContext(Dispatchers.IO) {
        val file = getVolumeConfigFile(userId, workId, volumeId)
        if (!file.exists()) return@withContext null
        
        try {
            val json = JSONObject(file.readText())
            return@withContext jsonToVolume(json)
        } catch (e: Exception) {
            return@withContext null
        }
    }

    /**
     * 更新卷信息
     * 对应 Vue 代码中的 updateVolume 方法
     */
    suspend fun updateVolume(userId: String, workId: String, volumeId: String, updates: Map<String, Any>): com.cwriter.data.model.Volume = withContext(Dispatchers.IO) {
        val configFile = getVolumeConfigFile(userId, workId, volumeId)
        val volumesListFile = getVolumesListFile(userId, workId)
        
        try {
            // 读取卷配置
            val volumeConfig = jsonToVolume(JSONObject(configFile.readText()))
            
            // 应用更新
            var updatedVolume = volumeConfig
            updates.forEach { (key, value) ->
                updatedVolume = when (key) {
                    "name" -> updatedVolume.copy(name = value as String, title = value as String)
                    "title" -> updatedVolume.copy(title = value as String, name = value as String)
                    "description" -> updatedVolume.copy(description = value as String)
                    else -> updatedVolume
                }
            }
            
            // 更新修改时间
            updatedVolume = updatedVolume.copy(updatedAt = nowISOString())
            
            // 保存卷配置
            configFile.writeText(volumeToJson(updatedVolume).toString())
            
            // 更新卷列表索引
            if (volumesListFile.exists()) {
                val jsonArray = JSONArray(volumesListFile.readText())
                val volumesList = mutableListOf<JSONObject>()
                
                for (i in 0 until jsonArray.length()) {
                    volumesList.add(jsonArray.getJSONObject(i))
                }
                
                val index = volumesList.indexOfFirst { it.optString("id") == volumeId }
                if (index >= 0) {
                    volumesList[index] = volumeToJson(updatedVolume)
                    volumesListFile.writeText(JSONArray(volumesList).toString())
                }
            }
            
            // 更新作品修改时间
            val work = getWork(userId, workId)
            if (work != null) {
                work.updatedAt = System.currentTimeMillis()
                updateWork(userId, work)
            }
            
            return@withContext updatedVolume
        } catch (e: Exception) {
            throw Exception("更新卷失败: ${e.message}")
        }
    }
    
    /**
     * 更新卷列表索引中的卷信息
     */
    private fun updateVolumeInIndex(userId: String, workId: String, volumeId: String, volume: com.cwriter.data.model.Volume) {
        try {
            val volumesListFile = getVolumesListFile(userId, workId)
            if (!volumesListFile.exists()) return
            
            val jsonArray = JSONArray(volumesListFile.readText())
            val volumesList = mutableListOf<JSONObject>()
            
            for (i in 0 until jsonArray.length()) {
                volumesList.add(jsonArray.getJSONObject(i))
            }
            
            val index = volumesList.indexOfFirst { it.optString("id") == volumeId }
            if (index >= 0) {
                volumesList[index] = volumeToJson(volume)
                volumesListFile.writeText(JSONArray(volumesList).toString())
            }
        } catch (e: Exception) {
            // 静默处理错误
        }
    }

    /**
     * 删除卷（需先删除卷内所有章节）
     * 对应 Vue 代码中的 deleteVolume 方法
     */
    suspend fun deleteVolume(userId: String, workId: String, volumeId: String) = withContext(Dispatchers.IO) {
        val configFile = getVolumeConfigFile(userId, workId, volumeId)
        val volumesListFile = getVolumesListFile(userId, workId)
        
        try {
            // 检查卷是否存在
            if (!configFile.exists()) {
                throw Exception("卷不存在")
            }
            
            // 检查卷内是否有章节
            val volumeConfig = jsonToVolume(JSONObject(configFile.readText()))
            if (volumeConfig.chapterCount > 0) {
                throw Exception("请先删除卷内所有章节")
            }
            
            // 从卷列表中移除
            if (volumesListFile.exists()) {
                val jsonArray = JSONArray(volumesListFile.readText())
                val volumesList = mutableListOf<JSONObject>()
                
                for (i in 0 until jsonArray.length()) {
                    volumesList.add(jsonArray.getJSONObject(i))
                }
                
                val filteredList = volumesList.filter { it.optString("id") != volumeId }
                
                // 重新排序
                val reorderedList = filteredList.mapIndexed { index, json ->
                    val volume = jsonToVolume(json)
                    volumeToJson(volume.copy(order = index + 1))
                }
                
                volumesListFile.writeText(JSONArray(reorderedList).toString())
            }
            
            // 删除卷文件夹（递归删除）
            val volumeDir = getVolumeDir(userId, workId, volumeId)
            volumeDir.deleteRecursively()
            
            // 更新作品修改时间
            val work = getWork(userId, workId)
            if (work != null) {
                work.updatedAt = System.currentTimeMillis()
                updateWork(userId, work)
            }
        } catch (e: Exception) {
            throw Exception("删除卷失败: ${e.message}")
        }
    }

    /**
     * 获取卷内章节列表
     * 对应 Vue 代码中的 getChaptersByVolume 方法
     */
    suspend fun getChaptersByVolume(userId: String, workId: String, volumeId: String): List<Chapter> = withContext(Dispatchers.IO) {
        val file = getVolumeChaptersListFile(userId, workId, volumeId)
        if (!file.exists()) return@withContext emptyList<Chapter>()
        
        try {
            val jsonArray = JSONArray(file.readText())
            val chapters = mutableListOf<Chapter>()
            
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                chapters.add(jsonToChapter(json))
            }
            
            return@withContext chapters
        } catch (e: Exception) {
            return@withContext emptyList<Chapter>()
        }
    }

    // ============ JSON 转换辅助方法 ============

    private fun jsonToVolume(json: JSONObject): com.cwriter.data.model.Volume {
        return com.cwriter.data.model.Volume(
            id = json.optString("id"),
            name = json.optString("name"),
            title = json.optString("title"),
            description = json.optString("description"),
            order = json.optInt("order"),
            chapterCount = json.optInt("chapter_count"),
            wordCount = json.optInt("word_count"),
            createdAt = json.optString("created_at"),
            updatedAt = json.optString("updated_at")
        )
    }

    /**
     * 为分卷作品创建章节
     * 对应 Vue 代码中的 createChapter 方法（分卷版本）
     */
    suspend fun createChapter(userId: String, workId: String, volumeId: String, chapter: Chapter): Chapter? = withContext(Dispatchers.IO) {
        try {
            // 确保章节有 volumeId
            val chapterWithVolumeId = chapter.copy(volumeId = volumeId)
            
            // 保存章节内容文件
            val chapterFile = getVolumeChapterFile(userId, workId, volumeId, chapter.id)
            chapterFile.writeText(chapterToJson(chapterWithVolumeId).toString())

            // 更新卷的章节列表
            val chapters = getChaptersByVolume(userId, workId, volumeId).toMutableList()
            chapters.add(chapterWithVolumeId)
            
            // 保存章节列表
            val chaptersListFile = getVolumeChaptersListFile(userId, workId, volumeId)
            val jsonArray = JSONArray()
            chapters.forEach { chapter ->
                jsonArray.put(chapterToJson(chapter))
            }
            chaptersListFile.writeText(jsonArray.toString())

            // 更新卷配置中的章节数
            val volume = getVolume(userId, workId, volumeId)
            if (volume != null) {
                val updatedVolume = volume.copy(
                    chapterCount = chapters.size,
                    updatedAt = nowISOString()
                )
                val configFile = getVolumeConfigFile(userId, workId, volumeId)
                configFile.writeText(volumeToJson(updatedVolume).toString())
                
                // 更新卷列表索引
                updateVolumeInIndex(userId, workId, volumeId, updatedVolume)
            }

            chapterWithVolumeId
        } catch (e: Exception) {
            throw Exception("创建章节失败: ${e.message}")
        }
    }
    
    private fun volumeToJson(volume: com.cwriter.data.model.Volume): JSONObject {
        return JSONObject().apply {
            put("id", volume.id)
            put("name", volume.name)
            put("title", volume.title)
            put("description", volume.description)
            put("order", volume.order)
            put("chapter_count", volume.chapterCount)
            put("word_count", volume.wordCount)
            put("created_at", volume.createdAt)
            put("updated_at", volume.updatedAt)
        }
    }

    // ============ 分卷章节扩展方法 ============

    /**
     * 获取单个分卷章节（含完整内容）
     * 对应 Vue 代码中的 getChapter(userId, workId, volumeId, chapterId)
     */
    suspend fun getChapter(userId: String, workId: String, volumeId: String, chapterId: String): Chapter? = withContext(Dispatchers.IO) {
        val file = getVolumeChapterFile(userId, workId, volumeId, chapterId)
        if (!file.exists()) return@withContext null
        try {
            jsonToChapter(JSONObject(file.readText()))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 更新分卷章节（含字数统计）
     * 对应 Vue 代码中的 updateChapter(userId, workId, volumeId, chapterId, updates)
     */
    suspend fun updateChapter(
        userId: String,
        workId: String,
        volumeId: String,
        chapterId: String,
        updates: Map<String, Any>
    ): Chapter = withContext(Dispatchers.IO) {
        val chapterFile = getVolumeChapterFile(userId, workId, volumeId, chapterId)
        val chaptersListFile = getVolumeChaptersListFile(userId, workId, volumeId)

        val chapter = if (chapterFile.exists())
            jsonToChapter(JSONObject(chapterFile.readText()))
        else
            throw Exception("章节不存在: $chapterId")

        var updated = chapter
        updates.forEach { (key, value) ->
            updated = when (key) {
                "title"        -> updated.copy(title = value as String)
                "content"      -> updated.copy(content = value as String)
                "is_completed" -> updated.copy(isCompleted = value as Boolean)
                "volume_order" -> updated.copy(volumeOrder = value as Int)
                "global_order" -> updated.copy(globalOrder = value as Int)
                else           -> updated
            }
        }
        // 重算字数
        updated = updated.copy(
            wordCount = updated.content.filter { !it.isWhitespace() }.length,
            updatedAt = System.currentTimeMillis()
        )

        // 保存完整章节文件
        chapterFile.writeText(chapterToJson(updated).toString())

        // 更新章节索引（不含 content）
        if (chaptersListFile.exists()) {
            val arr = JSONArray(chaptersListFile.readText())
            val list = (0 until arr.length()).map { arr.getJSONObject(it) }.toMutableList()
            val idx = list.indexOfFirst { it.optString("id") == chapterId }
            if (idx >= 0) {
                val indexJson = chapterToJson(updated).apply { put("content", "") }
                list[idx] = indexJson
                chaptersListFile.writeText(JSONArray(list).toString())
            }
        }

        // 更新卷统计
        updateVolumeStats(userId, workId, volumeId)

        // 更新作品总字数 + 修改时间
        val work = getWork(userId, workId)
        if (work != null) {
            val volumes = getVolumes(userId, workId)
            work.wordCount = volumes.sumOf { it.wordCount.toLong() }.toInt()
            work.chapterCount = volumes.sumOf { it.chapterCount }
            work.updatedAt = System.currentTimeMillis()
            updateWork(userId, work)
        }

        updated
    }

    /**
     * 获取所有章节（跨卷，按 globalOrder 排序）
     * 对应 Vue 代码中的 getAllChapters(userId, workId)
     */
    suspend fun getAllChapters(userId: String, workId: String): List<Chapter> = withContext(Dispatchers.IO) {
        val volumes = getVolumes(userId, workId)
        val all = mutableListOf<Chapter>()
        for (vol in volumes) {
            all += getChaptersByVolume(userId, workId, vol.id)
        }
        all.sortedBy { it.globalOrder }
    }

    /**
     * 卷重排序
     * 对应 Vue 代码中的 reorderVolumes(userId, workId, volumeOrder)
     * @param volumeOrder 新顺序的卷ID列表
     */
    suspend fun reorderVolumes(userId: String, workId: String, volumeOrder: List<String>) = withContext(Dispatchers.IO) {
        val volumesListFile = getVolumesListFile(userId, workId)
        if (!volumesListFile.exists()) return@withContext

        val arr = JSONArray(volumesListFile.readText())
        val list = (0 until arr.length()).map { jsonToVolume(arr.getJSONObject(it)) }.toMutableList()

        list.forEach { vol ->
            val newOrder = volumeOrder.indexOf(vol.id)
            if (newOrder >= 0) {
                val idx = list.indexOfFirst { it.id == vol.id }
                list[idx] = vol.copy(order = newOrder + 1, updatedAt = nowISOString())
            }
        }

        list.sortBy { it.order }
        val out = JSONArray()
        list.forEach { out.put(volumeToJson(it)) }
        volumesListFile.writeText(out.toString())
    }

    /**
     * 移动章节到其他卷
     * 对应 Vue 代码中的 moveChapterToVolume
     */
    suspend fun moveChapterToVolume(
        userId: String,
        workId: String,
        chapterId: String,
        targetVolumeId: String
    ) = withContext(Dispatchers.IO) {
        val allChapters = getAllChapters(userId, workId)
        val chapter = allChapters.find { it.id == chapterId }
            ?: throw Exception("章节不存在: $chapterId")

        val sourceVolumeId = chapter.volumeId
        if (sourceVolumeId == targetVolumeId) return@withContext

        // 在目标卷创建章节
        createChapter(userId, workId, targetVolumeId, chapter.copy(volumeId = targetVolumeId))

        // 删除原章节
        deleteVolumeChapter(userId, workId, sourceVolumeId, chapterId)

        // 重算全局序号
        recalculateGlobalOrder(userId, workId)
    }

    /**
     * 更新卷统计（章节数 + 字数）
     * 对应 Vue 代码中的 updateVolumeStats
     */
    suspend fun updateVolumeStats(userId: String, workId: String, volumeId: String) = withContext(Dispatchers.IO) {
        val chapters = getChaptersByVolume(userId, workId, volumeId)
        val chapterCount = chapters.size
        val wordCount = chapters.sumOf { it.wordCount }

        val configFile = getVolumeConfigFile(userId, workId, volumeId)
        if (!configFile.exists()) return@withContext

        val volume = jsonToVolume(JSONObject(configFile.readText()))
        val updated = volume.copy(
            chapterCount = chapterCount,
            wordCount = wordCount,
            updatedAt = nowISOString()
        )
        configFile.writeText(volumeToJson(updated).toString())
        updateVolumeInIndex(userId, workId, volumeId, updated)
    }

    /**
     * 重新计算所有章节的全局序号
     * 对应 Vue 代码中的 recalculateGlobalOrder
     */
    suspend fun recalculateGlobalOrder(userId: String, workId: String) = withContext(Dispatchers.IO) {
        val allChapters = getAllChapters(userId, workId)
        allChapters.forEachIndexed { index, chapter ->
            val newGlobal = index + 1
            if (chapter.globalOrder != newGlobal) {
                updateChapter(userId, workId, chapter.volumeId, chapter.id,
                    mapOf("global_order" to newGlobal))
            }
        }
    }

    /**
     * 获取用户统计（跨卷累计字数）
     * 覆盖原有实现，改为扫描所有卷内章节
     */
    suspend fun getUserStats(userId: String): UserStats = withContext(Dispatchers.IO) {
        val works = getWorks(userId)
        var totalWords = 0L
        var totalMaps = 0
        for (work in works) {
            val volumes = getVolumes(userId, work.id)
            if (volumes.isNotEmpty()) {
                // 分卷作品：累加各卷字数
                totalWords += volumes.sumOf { it.wordCount.toLong() }
            } else {
                // 非分卷作品：用 work.wordCount
                totalWords += work.wordCount
            }
            totalMaps += work.mapCount
        }
        UserStats(
            totalWorks = works.size,
            totalWords = totalWords,
            totalMaps = totalMaps
        )
    }

    // ============ 功能E/F/G 统一文件存储 ============

    /** 故事树数据文件路径 */
    private fun getNestedListFile(userId: String, workId: String): File =
        File(getWorkDir(userId, workId), "nested_list.json")

    /** 词库数据文件路径 */
    private fun getGlossaryFile(userId: String, workId: String): File =
        File(getWorkDir(userId, workId), "glossary.json")

    /** 伏笔数据文件路径 */
    private fun getForeshadowingFile(userId: String, workId: String): File =
        File(getWorkDir(userId, workId), "foreshadowings.json")

    /**
     * 读取故事树 JSON 字符串，文件不存在返回 null
     */
    suspend fun readNestedList(userId: String, workId: String): String? = withContext(Dispatchers.IO) {
        val file = getNestedListFile(userId, workId)
        if (!file.exists()) null else try { file.readText() } catch (_: Exception) { null }
    }

    /** 保存故事树 JSON 字符串 */
    suspend fun saveNestedList(userId: String, workId: String, json: String) = withContext(Dispatchers.IO) {
        getNestedListFile(userId, workId).writeText(json)
    }

    /**
     * 读取词库 JSON 字符串，文件不存在返回 null
     */
    suspend fun readGlossary(userId: String, workId: String): String? = withContext(Dispatchers.IO) {
        val file = getGlossaryFile(userId, workId)
        if (!file.exists()) null else try { file.readText() } catch (_: Exception) { null }
    }

    /** 保存词库 JSON 字符串 */
    suspend fun saveGlossary(userId: String, workId: String, json: String) = withContext(Dispatchers.IO) {
        getGlossaryFile(userId, workId).writeText(json)
    }

    /**
     * 读取伏笔 JSON 字符串，文件不存在返回 null
     */
    suspend fun readForeshadowings(userId: String, workId: String): String? = withContext(Dispatchers.IO) {
        val file = getForeshadowingFile(userId, workId)
        if (!file.exists()) null else try { file.readText() } catch (_: Exception) { null }
    }

    /** 保存伏笔 JSON 字符串 */
    suspend fun saveForeshadowings(userId: String, workId: String, json: String) = withContext(Dispatchers.IO) {
        getForeshadowingFile(userId, workId).writeText(json)
    }

}

