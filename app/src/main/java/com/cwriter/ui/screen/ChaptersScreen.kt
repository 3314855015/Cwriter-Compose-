package com.cwriter.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.Chapter
import com.cwriter.data.model.Work
import com.cwriter.data.repository.FileStorageRepository
import com.cwriter.ui.theme.DarkPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    userId: String,
    workId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String, String) -> Unit,
    viewModel: ChaptersViewModel = viewModel()
) {
    val context = LocalContext.current
    val work by viewModel.work.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Chapter?>(null) }

    LaunchedEffect(userId, workId) {
        viewModel.init(context, userId, workId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(work?.title ?: "章节列表") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加章节")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (chapters.isEmpty()) {
            EmptyChaptersState(
                onCreateClick = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chapters) { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        onClick = { onNavigateToEditor(workId, chapter.id) },
                        onDeleteClick = { showDeleteDialog = chapter }
                    )
                }
            }
        }
    }

    // 创建章节对话框
    if (showCreateDialog) {
        CreateChapterDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title ->
                viewModel.createChapter(title)
                showCreateDialog = false
            }
        )
    }

    // 删除确认对话框
    showDeleteDialog?.let { chapter ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除章节") },
            text = { Text("确定要删除「${chapter.title}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChapter(chapter.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun EmptyChaptersState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有章节",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮创建第一章",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkPrimary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("创建第一章")
        }
    }
}

@Composable
fun ChapterItem(
    chapter: Chapter,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${chapter.wordCount}字",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = chapter.getFormattedTime(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun CreateChapterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建新章节") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("章节标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

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
