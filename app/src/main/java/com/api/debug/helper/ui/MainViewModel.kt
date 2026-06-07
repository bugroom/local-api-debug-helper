package com.api.debug.helper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.api.debug.helper.data.*
import com.api.debug.helper.api.ApiService
import com.api.debug.helper.util.InAppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class UiState(
    val serverConfig: ServerConfig = ServerConfig(),
    val isModelsLoading: Boolean = false,
    val isChatLoading: Boolean = false,
    val healthStatus: HealthResponse? = null,
    val models: List<Model> = emptyList(),
    val chatResponse: String = "",
    val requestHistory: List<RequestHistory> = emptyList(),
    val error: String? = null,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED
)

 enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

 class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val history = mutableListOf<RequestHistory>()

    fun updateConfig(host: String, port: String, apiKey: String) {
        val portNum = port.toIntOrNull() ?: 8080
        _uiState.value = _uiState.value.copy(
            serverConfig = ServerConfig(host = host, port = portNum, apiKey = apiKey)
        )
    }

    fun testConnection() {
        InAppLogger.d("MainViewModel", "testConnection: Starting...")
        _uiState.value = _uiState.value.copy(
            status = ConnectionStatus.CONNECTING,
            error = null
        )

        viewModelScope.launch {
            try {
                val apiService = ApiService(_uiState.value.serverConfig)
                InAppLogger.d("MainViewModel", "testConnection: Calling health check...")
                
                apiService.healthCheck()
                    .onSuccess { health ->
                        InAppLogger.d("MainViewModel", "testConnection: Success - status=${health.status}")
                        _uiState.value = _uiState.value.copy(
                            healthStatus = health,
                            status = ConnectionStatus.CONNECTED,
                            error = null
                        )
                    }
                    .onFailure { e ->
                        InAppLogger.e("MainViewModel", "testConnection: Failed: ${e.message}", e)
                        _uiState.value = _uiState.value.copy(
                            status = ConnectionStatus.ERROR,
                            error = "连接失败: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                InAppLogger.e("MainViewModel", "testConnection: Exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    status = ConnectionStatus.ERROR,
                    error = "连接异常: ${e.message}"
                )
            }
        }
    }

    fun loadModels() {
        InAppLogger.d("MainViewModel", "loadModels: Starting...")
        _uiState.value = _uiState.value.copy(isModelsLoading = true)

        viewModelScope.launch {
            try {
                val apiService = ApiService(_uiState.value.serverConfig)
                InAppLogger.d("MainViewModel", "loadModels: Calling API...")

                apiService.listModels()
                    .onSuccess { modelsResponse ->
                        InAppLogger.d("MainViewModel", "loadModels: Success, ${modelsResponse.data.size} models")
                        _uiState.value = _uiState.value.copy(
                            models = modelsResponse.data,
                            isModelsLoading = false,
                            error = null
                        )
                    }
                    .onFailure { e ->
                        InAppLogger.e("MainViewModel", "loadModels: Failed: ${e.message}", e)
                        _uiState.value = _uiState.value.copy(
                            isModelsLoading = false,
                            error = "加载模型失败: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                InAppLogger.e("MainViewModel", "loadModels: Exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isModelsLoading = false,
                    error = "加载模型异常: ${e.message}"
                )
            }
        }
    }

    fun sendChatRequest(
        model: String,
        message: String,
        isStreaming: Boolean
    ) {
        InAppLogger.d("MainViewModel", "sendChatRequest: model=$model, streaming=$isStreaming")

        if (model.isBlank()) {
            InAppLogger.e("MainViewModel", "sendChatRequest: Model is blank!")
            _uiState.value = _uiState.value.copy(
                isChatLoading = false,
                error = "请先选择模型"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isChatLoading = true,
            chatResponse = "",
            error = null
        )

        viewModelScope.launch {
            try {
                val apiService = ApiService(_uiState.value.serverConfig)
                val request = ChatCompletionRequest(
                    model = model,
                    messages = listOf(ChatMessage("user", message)),
                    stream = isStreaming
                )

                val startTime = System.currentTimeMillis()
                var streamingCompleted = false
                var chunkReceived = false

                InAppLogger.d("MainViewModel", "sendChatRequest: Sending request...")

                apiService.chatCompletion(request) { chunk, done ->
                    if (!isStreaming) {
                        return@chatCompletion
                    }

                    if (done) {
                        InAppLogger.d("MainViewModel", "sendChatRequest: Stream completed")
                        streamingCompleted = true
                        _uiState.value = _uiState.value.copy(isChatLoading = false)

                        val responseBody = _uiState.value.chatResponse
                        addHistory(
                            endpoint = "/v1/chat/completions",
                            method = "POST",
                            requestBody = "model=$model, stream=$isStreaming",
                            responseBody = responseBody,
                            statusCode = 200
                        )
                    } else {
                        if (!chunkReceived) {
                            chunkReceived = true
                            InAppLogger.d("MainViewModel", "sendChatRequest: First chunk received")
                        }
                        _uiState.value = _uiState.value.copy(
                            chatResponse = _uiState.value.chatResponse + chunk
                        )
                    }
                }
                    .onSuccess { response ->
                        if (isStreaming) {
                            val duration = System.currentTimeMillis() - startTime
                            val responseBody = _uiState.value.chatResponse
                            InAppLogger.d("MainViewModel", "sendChatRequest: Streaming success, completed=$streamingCompleted, response length=${responseBody.length}, duration=${duration}ms")

                            if (!streamingCompleted) {
                                addHistory(
                                    endpoint = "/v1/chat/completions",
                                    method = "POST",
                                    requestBody = "model=$model, stream=$isStreaming",
                                    responseBody = responseBody,
                                    statusCode = 200
                                )
                            }

                            _uiState.value = _uiState.value.copy(
                                isChatLoading = false,
                                error = null
                            )
                            return@onSuccess
                        }

                        val duration = System.currentTimeMillis() - startTime
                        val responseBody = response.choices.firstOrNull()?.message?.content ?: ""
                        InAppLogger.d("MainViewModel", "sendChatRequest: Non-streaming success, response length=${responseBody.length}, duration=${duration}ms")

                        addHistory(
                            endpoint = "/v1/chat/completions",
                            method = "POST",
                            requestBody = "model=$model, stream=$isStreaming",
                            responseBody = responseBody,
                            statusCode = 200
                        )
                        _uiState.value = _uiState.value.copy(
                            chatResponse = responseBody,
                            isChatLoading = false,
                            error = null
                        )
                    }
                    .onFailure { e ->
                        InAppLogger.e("MainViewModel", "sendChatRequest: Failed: ${e.message}", e)
                        addHistory(
                            endpoint = "/v1/chat/completions",
                            method = "POST",
                            requestBody = "model=$model, stream=$isStreaming",
                            responseBody = e.message ?: "Unknown error",
                            statusCode = -1
                        )
                        _uiState.value = _uiState.value.copy(
                            isChatLoading = false,
                            error = "请求失败: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                InAppLogger.e("MainViewModel", "sendChatRequest: Exception: ${e.message}", e)
                addHistory(
                    endpoint = "/v1/chat/completions",
                    method = "POST",
                    requestBody = "model=$model, stream=$isStreaming",
                    responseBody = e.message ?: "Unknown error",
                    statusCode = -1
                )
                _uiState.value = _uiState.value.copy(
                    isChatLoading = false,
                    error = "请求异常: ${e.message}"
                )
            }
        }
    }

    fun clearChatResponse() {
        _uiState.value = _uiState.value.copy(chatResponse = "")
    }

    fun clearHistory() {
        history.clear()
        _uiState.value = _uiState.value.copy(requestHistory = emptyList())
    }

    private fun addHistory(
        endpoint: String,
        method: String,
        requestBody: String,
        responseBody: String,
        statusCode: Int
    ) {
        val record = RequestHistory(
            endpoint = endpoint,
            method = method,
            requestBody = requestBody,
            responseBody = responseBody,
            statusCode = statusCode
        )
        history.add(0, record)
        _uiState.value = _uiState.value.copy(requestHistory = history.toList())
    }
}
