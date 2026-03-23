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

/**
 * 章节列表 ViewModel - MVVM 架构
 * StateFlow 管理状态，协程处理异步数据加载
 */
class ChaptersViewModel : ViewModel() {
    private var repository: FileStorageRepository? = null
    private var userId: String = "default_user"
    private var workId: String = ""

    private val _work = MutableStateFlow<Work?>(null)
    val work: StateFlow<Work?> = _work.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun init(context: Context, uid: String, wid: String) {
        repository = FileStorageRepository(context)
        userId = uid
        workId = wid
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _work.value = repository?.getWork(userId, workId)
            _chapters.value = repository?.getChapters(userId, workId) ?: emptyList()
            _isLoading.value = false
        }
    }

    fun createChapter(title: String) {
        viewModelScope.launch {
            val chapter = Chapter(title = title)
            repository?.createChapter(userId, workId, chapter)
            loadData()
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            repository?.deleteChapter(userId, workId, chapterId)
            loadData()
        }
    }
}
