package com.cwriter.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cwriter.data.model.UserStats
import com.cwriter.data.model.Work
import com.cwriter.data.repository.FileStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel - MVVM 架构
 * 使用 StateFlow 实现响应式状态管理，协程处理异步操作
 */
class HomeViewModel : ViewModel() {

    private var repository: FileStorageRepository? = null
    private var userId: String = "default_user"

    private val _works = MutableStateFlow<List<Work>>(emptyList())
    val works: StateFlow<List<Work>> = _works.asStateFlow()

    private val _stats = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun init(context: Context, uid: String) {
        repository = FileStorageRepository(context)
        userId = uid
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository?.let { repo ->
                _works.value = repo.getWorks(userId)
                _stats.value = repo.getUserStats(userId)
            }
            _isLoading.value = false
        }
    }

    fun createWork(title: String, description: String, structureType: Work.StructureType) {
        viewModelScope.launch {
            val work = Work(
                title = title,
                description = description,
                structureType = structureType
            )
            repository?.createWork(userId, work)
            loadData()
        }
    }

    fun toggleFavorite(work: Work) {
        viewModelScope.launch {
            work.isFavorite = !work.isFavorite
            repository?.updateWork(userId, work)
            loadData()
        }
    }

    fun deleteWork(workId: String) {
        viewModelScope.launch {
            repository?.deleteWork(userId, workId)
            loadData()
        }
    }
}
