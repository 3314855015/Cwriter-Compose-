package com.cwriter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Work
import com.cwriter.data.repository.FileStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EditorState { A, B, C }
enum class ReadMode { SCROLL, PAGE }

class ChapterEditorViewModel : ViewModel() {
    private var repository: FileStorageRepository? = null
    private var userId: String = "default_user"
    private var workId: String = ""
    private var chapterId: String = ""
    private var volumeId: String = ""

    // 章节导航用：所有章节的 (chapterId, volumeId) 对
    private var allChapterPairs: List<Pair<String, String>> = emptyList()

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
        userId = uid
        workId = wid
        chapterId = cid
        volumeId = vid
        loadData()
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
        _showForeshadowingPanel.value = !_showForeshadowingPanel.value
    }

    fun toggleGlossaryPanel() {
        val opening = !_showGlossaryPanel.value
        _showGlossaryPanel.value = opening
        if (opening) _showNestedListPanel.value = false
    }

    fun closeAllPanels() {
        _showNestedListPanel.value = false
        _showForeshadowingPanel.value = false
        _showGlossaryPanel.value = false
        _showTextStylePanel.value = false
    }

    // 返回 (chapterId, volumeId)
    fun navigateToPrevChapter(onNavigate: (String, String) -> Unit) {
        val idx = allChapterPairs.indexOfFirst { it.first == chapterId }
        if (idx > 0) {
            val (cid, vid) = allChapterPairs[idx - 1]
            onNavigate(cid, vid)
        }
    }

    fun navigateToNextChapter(onNavigate: (String, String) -> Unit) {
        val idx = allChapterPairs.indexOfFirst { it.first == chapterId }
        if (idx < allChapterPairs.size - 1) {
            val (cid, vid) = allChapterPairs[idx + 1]
            onNavigate(cid, vid)
        }
    }

    fun showSnackbar(message: String) { _snackbarMessage.value = message }
    fun clearSnackbar() { _snackbarMessage.value = "" }
}
