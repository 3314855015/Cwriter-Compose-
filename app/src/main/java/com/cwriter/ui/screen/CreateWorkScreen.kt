package com.cwriter.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cwriter.data.model.Work
import com.cwriter.data.repository.FileStorageRepository
import com.cwriter.ui.theme.DarkPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onWorkCreated: () -> Unit,
    viewModel: CreateWorkViewModel = viewModel()
) {
    val context = LocalContext.current
    val isCreated by viewModel.isCreated.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var structureType by remember { mutableStateOf(Work.StructureType.VOLUMED) }

    LaunchedEffect(userId) {
        viewModel.init(context, userId)
    }

    LaunchedEffect(isCreated) {
        if (isCreated) {
            onWorkCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建作品") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.createWork(title, description, structureType)
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "确认")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 作品标题
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("作品标题 *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 作品简介
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("作品简介（可选）") },
                minLines = 4,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 结构类型
            Text(
                text = "结构类型",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TypeCard(
                    title = "分卷",
                    description = "适合长篇小说、文集等需要分卷管理的作品",
                    selected = structureType == Work.StructureType.VOLUMED,
                    onClick = { structureType = Work.StructureType.VOLUMED },
                    modifier = Modifier.weight(1f)
                )
                TypeCard(
                    title = "整体",
                    description = "适合短篇、散文、日记等单文档作品",
                    selected = structureType == Work.StructureType.SINGLE,
                    onClick = { structureType = Work.StructureType.SINGLE },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 创建按钮
            Button(
                onClick = {
                    viewModel.createWork(title, description, structureType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPrimary
                )
            ) {
                Text("创建作品", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TypeCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                DarkPrimary.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) DarkPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

class CreateWorkViewModel : ViewModel() {
    private var repository: FileStorageRepository? = null
    private var userId: String = "default_user"

    private val _isCreated = MutableStateFlow(false)
    val isCreated: StateFlow<Boolean> = _isCreated.asStateFlow()

    fun init(context: Context, uid: String) {
        repository = FileStorageRepository(context)
        userId = uid
    }

    fun createWork(title: String, description: String, structureType: Work.StructureType) {
        viewModelScope.launch {
            val work = Work(
                title = title,
                description = description,
                structureType = structureType
            )
            repository?.createWork(userId, work)
            _isCreated.value = true
        }
    }
}
