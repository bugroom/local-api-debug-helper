package com.api.debug.helper.ui

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.api.debug.helper.R
import com.api.debug.helper.data.Model
import com.api.debug.helper.util.InAppLogger
import com.api.debug.helper.util.LogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Server Configuration
            ServerConfigSection(
                config = uiState.serverConfig,
                status = uiState.status,
                onConfigChanged = { host, port, key ->
                    viewModel.updateConfig(host, port, key)
                },
                onTestConnection = { viewModel.testConnection() }
            )

            Divider()

            // Tab Navigation
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("健康") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("模型") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("聊天") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("历史") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("日志") }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> HealthCheckTab(uiState, viewModel)
                1 -> ModelsListTab(uiState, viewModel)
                2 -> ChatCompletionTab(uiState, viewModel)
                3 -> RequestHistoryTab(uiState, viewModel)
                4 -> LogsTab()
            }

            // Error Display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServerConfigSection(
    config: com.api.debug.helper.data.ServerConfig,
    status: ConnectionStatus,
    onConfigChanged: (String, String, String) -> Unit,
    onTestConnection: () -> Unit
) {
    var host by remember { mutableStateOf(config.host) }
    var port by remember { mutableStateOf(config.port.toString()) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var expanded by remember { mutableStateOf(status != ConnectionStatus.CONNECTED) }

    LaunchedEffect(status) {
        if (status == ConnectionStatus.CONNECTED) {
            expanded = false
        }
    }

    val statusText = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.connected)
        ConnectionStatus.DISCONNECTED -> stringResource(R.string.disconnected)
        ConnectionStatus.CONNECTING -> stringResource(R.string.connecting)
        ConnectionStatus.ERROR -> stringResource(R.string.error)
    }

    if (!expanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "API: ${config.host}:${config.port}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "状态: $statusText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedButton(onClick = { expanded = true }) {
                Text("配置")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.server_config),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = host,
            onValueChange = { host = it; onConfigChanged(host, port, apiKey) },
            label = { Text(stringResource(R.string.server_host)) },
            placeholder = { Text(stringResource(R.string.enter_host)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = port,
            onValueChange = { port = it; onConfigChanged(host, port, apiKey) },
            label = { Text(stringResource(R.string.server_port)) },
            placeholder = { Text(stringResource(R.string.enter_port)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; onConfigChanged(host, port, apiKey) },
            label = { Text(stringResource(R.string.api_key)) },
            placeholder = { Text(stringResource(R.string.enter_api_key)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${stringResource(R.string.status)}: $statusText",
                style = MaterialTheme.typography.bodyMedium,
                color = when (status) {
                    ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                    ConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status == ConnectionStatus.CONNECTED) {
                    OutlinedButton(onClick = { expanded = false }) {
                        Text("收起")
                    }
                }
                Button(onClick = onTestConnection) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
fun HealthCheckTab(
    uiState: UiState,
    viewModel: MainViewModel
) {
    val health = uiState.healthStatus

    if (health == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { viewModel.testConnection() }) {
                Text(stringResource(R.string.health_check))
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.status),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Status: ${health.status}")
                    Text("Uptime: ${health.uptime / 1000}s")
                    Text("Connections: ${health.connections}")
                    health.loaded_model?.let {
                        Text("Loaded Model: $it")
                    }
                }
            }
        }
    }
}

@Composable
fun ModelsListTab(
    uiState: UiState,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.list_models),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { viewModel.loadModels() },
                enabled = !uiState.isModelsLoading
            ) {
                if (uiState.isModelsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.list_models))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.models.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_models))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.models) { model ->
                    ModelCard(model)
                }
            }
        }
    }
}

@Composable
fun ModelCard(model: Model) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                model.id,
                style = MaterialTheme.typography.titleMedium
            )
            Text("Owner: ${model.owned_by}")
            Text("Created: ${model.created}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatCompletionTab(
    uiState: UiState,
    viewModel: MainViewModel
) {
    var selectedModel by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (uiState.models.isNotEmpty() && selectedModel.isBlank()) {
                selectedModel = uiState.models.first().id
            }

            Text("模型", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.models.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.models.find { it.id == selectedModel }?.id ?: "",
                        onValueChange = {},
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )

                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        uiState.models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.id) },
                                onClick = {
                                    selectedModel = model.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text("请先到模型页加载模型列表", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Text("消息", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                    cursorBrush = SolidColor(Color.Black),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (message.isBlank()) {
                                Text("输入消息", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isStreaming, onCheckedChange = { isStreaming = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.streaming))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (selectedModel.isNotBlank() && message.isNotBlank()) {
                            viewModel.sendChatRequest(selectedModel, message, isStreaming)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isChatLoading && selectedModel.isNotBlank() && message.isNotBlank()
                ) {
                    if (uiState.isChatLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("发送中")
                        }
                    } else {
                        Text(stringResource(R.string.send))
                    }
                }

                OutlinedButton(
                    onClick = {
                        message = ""
                        viewModel.clearChatResponse()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.clear))
                }
            }
        }

        if (uiState.chatResponse.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.response), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            uiState.chatResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestHistoryTab(
    uiState: UiState,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.request_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = { viewModel.clearHistory() }) {
                Text(stringResource(R.string.clear))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.requestHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无历史记录")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.requestHistory) { history ->
                    HistoryCard(history)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(history: com.api.debug.helper.data.RequestHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (history.statusCode == 200) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${history.method} ${history.endpoint}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "HTTP ${history.statusCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (history.statusCode == 200) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                history.requestBody,
                style = MaterialTheme.typography.bodySmall
            )
            Divider()
            Text(
                history.responseBody,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
fun LogsTab() {
    val logs by InAppLogger.logFlow.collectAsState()
    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "运行日志 (${logs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                OutlinedButton(
                    onClick = { 
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("API Debug Logs", InAppLogger.getAllLogs())
                        clipboard.setPrimaryClip(clip)
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("复制")
                }
                OutlinedButton(
                    onClick = { InAppLogger.clear() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("清除")
                }
                OutlinedButton(onClick = { autoScroll = !autoScroll }) {
                    Text(if (autoScroll) "暂停滚动" else "自动滚动")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs) { log ->
                LogItem(log)
            }
        }
    }
    
    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val color = when (log.level) {
        "E" -> MaterialTheme.colorScheme.error
        "W" -> MaterialTheme.colorScheme.tertiary
        "D" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Text(
        text = log.formatted(),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.fillMaxWidth()
    )
}
