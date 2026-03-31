package com.cwriter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Foreshadowing
import com.cwriter.data.model.ForeshadowingStatus
import com.cwriter.data.model.Work
import com.cwriter.data.repository.FileStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class EditorState { A, B, C }
enum class ReadMode { SCROLL, PAGE }

class ChapterEditorViewModel : ViewModel() {
    private var repository: FileStorageRepository? = null
    private var context: Context? = null
    private var userId: String = "default_user"
    private var workId: String = ""
    private var chapterId: String = ""
    private var volumeId: String = ""

    // 章节导航用：所有章节的 (chapterId, volumeId) 对
    private var allChapterPairs: List<Pair<String, String>> = emptyList()

    private val _allChapterPairs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val allChapterPairsFlow: StateFlow<List<Pair<String, String>>> = _allChapterPairs.asStateFlow()

    private val _work = MutableStateFlow<Work?>(null)
    val work: StateFlow<Work?> = _work.asStateFlow()

    private val _chapter = MutableStateFlow<Chapter?>(null)
    val chapter: StateFlow<Chapter?> = _chapter.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState.A)
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow("")
    val snackbarMessage: StateFlow<String> = _snackbarMessage.asStateFlow()

    private val _readMode = MutableStateFlow(ReadMode.SCROLL)
    val readMode: StateFlow<ReadMode> = _readMode.asStateFlow()

    private val _localDarkMode = MutableStateFlow(true)
    val localDarkMode: StateFlow<Boolean> = _localDarkMode.asStateFlow()

    private val _fontSize = MutableStateFlow(16f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    // 词库插入：非 null 时 EditorContent 在光标位置插入文字后清空
    private val _pendingInsertText = MutableStateFlow<String?>(null)
    val pendingInsertText: StateFlow<String?> = _pendingInsertText.asStateFlow()

    fun requestInsertText(text: String) { _pendingInsertText.value = text }
    fun clearPendingInsert() { _pendingInsertText.value = null }

    private val _lineHeight = MutableStateFlow(1.8f)
    val lineHeight: StateFlow<Float> = _lineHeight.asStateFlow()

    private val _showTextStylePanel = MutableStateFlow(false)
    val showTextStylePanel: StateFlow<Boolean> = _showTextStylePanel.asStateFlow()

    private val _showNestedListPanel = MutableStateFlow(false)
    val showNestedListPanel: StateFlow<Boolean> = _showNestedListPanel.asStateFlow()

    private val _showForeshadowingPanel = MutableStateFlow(false)
    val showForeshadowingPanel: StateFlow<Boolean> = _showForeshadowingPanel.asStateFlow()

    private val _showGlossaryPanel = MutableStateFlow(false)
    val showGlossaryPanel: StateFlow<Boolean> = _showGlossaryPanel.asStateFlow()

    private val _hasPrevChapter = MutableStateFlow(false)
    val hasPrevChapter: StateFlow<Boolean> = _hasPrevChapter.asStateFlow()

    private val _hasNextChapter = MutableStateFlow(false)
    val hasNextChapter: StateFlow<Boolean> = _hasNextChapter.asStateFlow()

    // 撤销/重做栈
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun init(context: Context, uid: String, wid: String, cid: String, vid: String = "") {
        repository = FileStorageRepository(context)
        this.context = context
        userId = uid
        workId = wid
        chapterId = cid
        volumeId = vid
        loadData()
        loadForeshadowings()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _work.value = repository?.getWork(userId, workId)

            // 优先用分卷接口（volumeId 非空），否则降级到旧接口
            _chapter.value = if (volumeId.isNotEmpty()) {
                repository?.getChapter(userId, workId, volumeId, chapterId)
            } else {
                repository?.getChapter(userId, workId, chapterId)
            }

            // 构建章节导航列表（跨卷连续）
            try {
                val volumes = repository?.getVolumes(userId, workId) ?: emptyList()
                if (volumes.isNotEmpty()) {
                    val pairs = mutableListOf<Pair<String, String>>()
                    for (vol in volumes) {
                        val chapters = repository?.getChaptersByVolume(userId, workId, vol.id) ?: emptyList()
                        chapters.forEach { pairs.add(it.id to vol.id) }
                    }
                    allChapterPairs = pairs
                } else {
                    // 旧结构兼容
                    val chapters = repository?.getChapters(userId, workId) ?: emptyList()
                    allChapterPairs = chapters.map { it.id to "" }
                }
                _allChapterPairs.value = allChapterPairs
                val idx = allChapterPairs.indexOfFirst { it.first == chapterId }
                _hasPrevChapter.value = idx > 0
                _hasNextChapter.value = idx < allChapterPairs.size - 1
            } catch (e: Exception) {
                _hasPrevChapter.value = false
                _hasNextChapter.value = false
            }

            _isLoading.value = false
        }
    }

    fun showToolbar() { _editorState.value = EditorState.B }
    fun hideToolbar() { _editorState.value = EditorState.A }

    fun enterEditMode() {
        _editorState.value = EditorState.C
        _readMode.value = ReadMode.SCROLL
        undoStack.clear()
        redoStack.clear()
    }

    fun exitEditMode() {
        saveChapter()
        _editorState.value = EditorState.B
        closeAllPanels()
    }

    fun updateContent(newContent: String) {
        val current = _chapter.value ?: return
        undoStack.add(current.content)
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
        _chapter.value = current.copy(
            content = newContent,
            wordCount = newContent.filter { !it.isWhitespace() }.length
        )
    }

    fun updateTitle(newTitle: String) {
        val current = _chapter.value ?: return
        _chapter.value = current.copy(title = newTitle)
    }

    fun undo() {
        if (!canUndo) return
        val current = _chapter.value ?: return
        redoStack.add(current.content)
        val previous = undoStack.removeLast()
        _chapter.value = current.copy(
            content = previous,
            wordCount = previous.filter { !it.isWhitespace() }.length
        )
    }

    fun redo() {
        if (!canRedo) return
        val current = _chapter.value ?: return
        undoStack.add(current.content)
        val next = redoStack.removeLast()
        _chapter.value = current.copy(
            content = next,
            wordCount = next.filter { !it.isWhitespace() }.length
        )
    }

    fun autoIndent() {
        val current = _chapter.value ?: return
        val lines = current.content.split("\n")
        val formatted = lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("　　")) "　　$trimmed" else line
        }
        updateContent(formatted)
        showSnackbar("已自动缩进")
    }

    fun saveChapter() {
        viewModelScope.launch {
            val chapter = _chapter.value ?: return@launch
            try {
                if (volumeId.isNotEmpty()) {
                    repository?.updateChapter(
                        userId, workId, volumeId, chapter.id,
                        mapOf("title" to chapter.title, "content" to chapter.content)
                    )
                } else {
                    repository?.updateChapter(userId, workId, chapter)
                }
                showSnackbar("已保存")
            } catch (e: Exception) {
                showSnackbar("保存失败: ${e.message}")
            }
        }
    }

    fun toggleReadMode() {
        _readMode.value = if (_readMode.value == ReadMode.SCROLL) ReadMode.PAGE else ReadMode.SCROLL
    }

    fun toggleLocalTheme() { _localDarkMode.value = !_localDarkMode.value }

    fun setFontSize(size: Float) { _fontSize.value = size.coerceIn(12f, 28f) }
    fun setLineHeight(height: Float) { _lineHeight.value = height.coerceIn(1.2f, 3.0f) }
    fun toggleTextStylePanel() { _showTextStylePanel.value = !_showTextStylePanel.value }
    fun hideTextStylePanel() { _showTextStylePanel.value = false }

    fun toggleNestedListPanel() {
        val opening = !_showNestedListPanel.value
        _showNestedListPanel.value = opening
        if (opening) _showGlossaryPanel.value = false
    }

    fun toggleForeshadowingPanel() {
        if (_editorState.value == EditorState.C) {
            showSnackbar("请在阅读模式下使用伏笔功能")
            return
        }
        _showForeshadowingPanel.value = !_showForeshadowingPanel.value
    }

    // 伏笔底部弹窗（点击图标时打开）
    private val _showForeshadowingSheet = MutableStateFlow(false)
    val showForeshadowingSheet: StateFlow<Boolean> = _showForeshadowingSheet.asStateFlow()

    // ── 伏笔数据 ─────────────────────────────────────────────────────────────

    private val _foreshadowings = MutableStateFlow<List<Foreshadowing>>(emptyList())
    val foreshadowings: StateFlow<List<Foreshadowing>> = _foreshadowings.asStateFlow()

    // 当前点击的段落索引（用于打开 BottomSheet）
    private val _selectedParagraphIndex = MutableStateFlow(0)
    val selectedParagraphIndex: StateFlow<Int> = _selectedParagraphIndex.asStateFlow()

    fun openForeshadowingSheet(paragraphIndex: Int) {
        _selectedParagraphIndex.value = paragraphIndex
        _showForeshadowingSheet.value = true
    }

    fun closeForeshadowingSheet() {
        _showForeshadowingSheet.value = false
    }

    private fun loadForeshadowings() {
        val ctx = context ?: return
        val repo = repository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // 优先从文件系统读取
            var list = emptyList<Foreshadowing>()
            val fileContent = repo.readForeshadowings(userId, workId)
            if (fileContent != null) {
                list = try { jsonToForeshadowings(JSONArray(fileContent)) }
                catch (e: Exception) { emptyList() }
            } else {
                // 文件不存在 → 尝试从 SP 迁移
                val key = "foreshadowing_$workId"
                val spStr = ctx.getSharedPreferences("cwriter_foreshadowing", Context.MODE_PRIVATE)
                    .getString(key, null)
                if (spStr != null) {
                    list = try { jsonToForeshadowings(JSONArray(spStr)) }
                    catch (e: Exception) { emptyList() }
                    if (list.isNotEmpty()) {
                        val arr = foreshadowingsToJson(list)
                        repo.saveForeshadowings(userId, workId, arr.toString())
                        ctx.getSharedPreferences("cwriter_foreshadowing", Context.MODE_PRIVATE)
                            .edit().remove(key).apply()
                    }
                }
            }
            withContext(Dispatchers.Main) { _foreshadowings.value = list }
        }
    }

    private fun saveForeshadowings() {
        val repo = repository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val arr = foreshadowingsToJson(_foreshadowings.value)
            repo.saveForeshadowings(userId, workId, arr.toString())
        }
    }

    fun createForeshadowing(paragraphIndex: Int, content: String) {
        val f = Foreshadowing(
            workId               = workId,
            chapterId            = chapterId,
            createdParagraphIndex = paragraphIndex,
            content              = content
        )
        _foreshadowings.value = _foreshadowings.value + f
        saveForeshadowings()
    }

    fun recycleForeshadowing(foreshadowingId: String) {
        _foreshadowings.value = _foreshadowings.value.map { f ->
            if (f.id == foreshadowingId) f.copy(
                status                = ForeshadowingStatus.RECYCLED,
                recycledChapterId     = chapterId,
                recycledParagraphIndex = _selectedParagraphIndex.value,
                recycledAt            = System.currentTimeMillis()
            ) else f
        }
        saveForeshadowings()
    }

    fun unrecycleForeshadowing(foreshadowingId: String) {
        _foreshadowings.value = _foreshadowings.value.map { f ->
            if (f.id == foreshadowingId) f.copy(
                status                = ForeshadowingStatus.PENDING,
                recycledChapterId     = null,
                recycledParagraphIndex = null,
                recycledAt            = null
            ) else f
        }
        saveForeshadowings()
    }

    // ── JSON 序列化 ───────────────────────────────────────────────────────────

    private fun foreshadowingsToJson(list: List<Foreshadowing>): JSONArray = JSONArray().also { arr ->
        list.forEach { f ->
            arr.put(JSONObject().apply {
                put("id",                    f.id)
                put("workId",                f.workId)
                put("chapterId",             f.chapterId)
                put("createdParagraphIndex", f.createdParagraphIndex)
                put("content",               f.content)
                put("status",                f.status.name)
                put("createdAt",             f.createdAt)
                f.recycledChapterId?.let     { put("recycledChapterId", it) }
                f.recycledParagraphIndex?.let { put("recycledParagraphIndex", it) }
                f.recycledAt?.let            { put("recycledAt", it) }
            })
        }
    }

    private fun jsonToForeshadowings(arr: JSONArray): List<Foreshadowing> =
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            Foreshadowing(
                id                    = obj.optString("id"),
                workId                = obj.optString("workId"),
                chapterId             = obj.optString("chapterId"),
                createdParagraphIndex = obj.optInt("createdParagraphIndex"),
                content               = obj.optString("content"),
                status                = try { ForeshadowingStatus.valueOf(obj.optString("status")) }
                                        catch (e: Exception) { ForeshadowingStatus.PENDING },
                createdAt             = obj.optLong("createdAt"),
                recycledChapterId     = obj.optString("recycledChapterId").ifEmpty { null },
                recycledParagraphIndex = if (obj.has("recycledParagraphIndex")) obj.optInt("recycledParagraphIndex") else null,
                recycledAt            = if (obj.has("recycledAt")) obj.optLong("recycledAt") else null
            )
        }

    fun toggleGlossaryPanel() {
        val opening = !_showGlossaryPanel.value
        _showGlossaryPanel.value = opening
        if (opening) _showNestedListPanel.value = false
    }

    fun closeAllPanels() {
        _showNestedListPanel.value = false
        _showForeshadowingPanel.value = false
        _showForeshadowingSheet.value = false
        _showGlossaryPanel.value = false
        _showTextStylePanel.value = false
    }

    // 新建章节弹窗触发（最后一章点下一章时）
    private val _showCreateChapterDialog = MutableStateFlow(false)
    val showCreateChapterDialog: StateFlow<Boolean> = _showCreateChapterDialog.asStateFlow()

    fun dismissCreateChapterDialog() { _showCreateChapterDialog.value = false }

    /** 在当前卷末尾新建章节，创建后导航过去 */
    fun createChapterAndNavigate(title: String, onNavigate: (String, String) -> Unit) {
        viewModelScope.launch {
            try {
                val newChapter = com.cwriter.data.model.Chapter(
                    title    = title.ifBlank { "新章节" },
                    content  = "",
                    volumeId = volumeId
                )
                val created = if (volumeId.isNotEmpty()) {
                    repository?.createChapter(userId, workId, volumeId, newChapter)
                } else null
                if (created != null) {
                    _showCreateChapterDialog.value = false
                    onNavigate(created.id, volumeId)
                } else {
                    showSnackbar("新建章节失败")
                }
            } catch (e: Exception) {
                showSnackbar("新建章节失败: ${e.message}")
            }
        }
    }

    // 返回 (chapterId, volumeId)
    fun navigateToPrevChapter(onNavigate: (String, String) -> Unit) {
        val idx = allChapterPairs.indexOfFirst { it.first == chapterId }
        if (idx > 0) {
            val (cid, vid) = allChapterPairs[idx - 1]
            onNavigate(cid, vid)
        } else {
            showSnackbar("没有上一章了")
        }
    }

    fun navigateToNextChapter(onNavigate: (String, String) -> Unit) {
        val idx = allChapterPairs.indexOfFirst { it.first == chapterId }
        if (idx >= 0 && idx < allChapterPairs.size - 1) {
            val (cid, vid) = allChapterPairs[idx + 1]
            onNavigate(cid, vid)
        } else {
            // allChapterPairs 只含有章节的卷，所以这里就是真正的最后一章
            // 空卷不影响判断，直接弹新建章节
            _showCreateChapterDialog.value = true
        }
    }

    fun showSnackbar(message: String) { _snackbarMessage.value = message }
    fun clearSnackbar() { _snackbarMessage.value = "" }
}
