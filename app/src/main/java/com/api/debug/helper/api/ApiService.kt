package com.api.debug.helper.api

import com.api.debug.helper.data.*
import com.api.debug.helper.util.InAppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiService(private val config: ServerConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private fun getHeaders(): Headers {
        return if (config.apiKey.isNotBlank()) {
            Headers.headersOf("Authorization", "Bearer ${config.apiKey}")
        } else {
            Headers.headersOf()
        }
    }

    suspend fun healthCheck(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            InAppLogger.d("ApiService", "healthCheck: Requesting ${config.baseUrl}/health")
            
            val request = Request.Builder()
                .url("${config.baseUrl}/health")
                .headers(getHeaders())
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            InAppLogger.d("ApiService", "healthCheck: Response code=${response.code}, body=$body")

            if (response.isSuccessful) {
                val healthResponse = json.decodeFromString<HealthResponse>(body)
                InAppLogger.d("ApiService", "healthCheck: Success - status=${healthResponse.status}")
                Result.success(healthResponse)
            } else {
                InAppLogger.e("ApiService", "healthCheck: Failed HTTP ${response.code}: $body")
                Result.failure(IOException("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            InAppLogger.e("ApiService", "healthCheck: Exception ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun listModels(): Result<ModelsResponse> = withContext(Dispatchers.IO) {
        try {
            InAppLogger.d("ApiService", "listModels: Requesting ${config.baseUrl}/v1/models")
            
            val request = Request.Builder()
                .url("${config.baseUrl}/v1/models")
                .headers(getHeaders())
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            InAppLogger.d("ApiService", "listModels: Response code=${response.code}")

            if (response.isSuccessful) {
                val modelsResponse = json.decodeFromString<ModelsResponse>(body)
                InAppLogger.d("ApiService", "listModels: Success, ${modelsResponse.data.size} models")
                Result.success(modelsResponse)
            } else {
                InAppLogger.e("ApiService", "listModels: Failed HTTP ${response.code}: $body")
                Result.failure(IOException("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            InAppLogger.e("ApiService", "listModels: Exception ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun chatCompletion(
        request: ChatCompletionRequest,
        onChunk: ((String, Boolean) -> Unit)? = null
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        try {
            InAppLogger.d("ApiService", "chatCompletion: model=${request.model}, stream=${request.stream}")
            
            val requestBody = json.encodeToString(ChatCompletionRequest.serializer(), request)
                .toRequestBody(mediaType)
            
            InAppLogger.d("ApiService", "chatCompletion: Request body sent")

            val httpRequest = Request.Builder()
                .url("${config.baseUrl}/v1/chat/completions")
                .headers(getHeaders())
                .post(requestBody)
                .build()

            val response = client.newCall(httpRequest).execute()
            
            InAppLogger.d("ApiService", "chatCompletion: Response code=${response.code}")

            if (request.stream) {
                InAppLogger.d("ApiService", "chatCompletion: Handling streaming response")
                handleStreamingResponse(response, onChunk ?: { _, _ -> })
                Result.success(ChatCompletionResponse(
                    id = "stream",
                    `object` = "chat.completion",
                    created = System.currentTimeMillis() / 1000,
                    model = request.model,
                    choices = emptyList(),
                    usage = null
                ))
            } else {
                val body = response.body?.string() ?: ""
                InAppLogger.d("ApiService", "chatCompletion: Non-streaming body received")
                if (response.isSuccessful) {
                    val chatResponse = decodeChatCompletionResponse(body, request.model)
                    Result.success(chatResponse)
                } else {
                    InAppLogger.e("ApiService", "chatCompletion: Failed HTTP ${response.code}: $body")
                    Result.failure(IOException("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            InAppLogger.e("ApiService", "chatCompletion: Exception ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun decodeChatCompletionResponse(body: String, model: String): ChatCompletionResponse {
        return try {
            json.decodeFromString<ChatCompletionResponse>(body)
        } catch (e: Exception) {
            InAppLogger.e("ApiService", "chatCompletion: standard decode failed, trying fallback: ${e.message}")
            val content = extractResponseText(body)
            ChatCompletionResponse(
                model = model,
                choices = listOf(
                    Choice(
                        message = ChatMessage(role = "assistant", content = content),
                        finish_reason = "stop"
                    )
                )
            )
        }
    }

    private fun extractResponseText(body: String): String {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            root.stringField("response")
                ?: root.stringField("content")
                ?: root.stringField("text")
                ?: root.stringField("message")
                ?: root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("message")?.jsonObject?.stringField("content")
                ?: root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.stringField("text")
                ?: body
        } catch (_: Exception) {
            body
        }
    }

    private fun JsonObject.stringField(name: String): String? {
        return (this[name] as? JsonPrimitive)?.jsonPrimitive?.contentOrNull
    }

    private fun handleStreamingResponse(
        response: Response,
        onChunk: (String, Boolean) -> Unit
    ) {
        var chunkCount = 0
        var parseErrorCount = 0
        
        response.body?.use { body ->
            val reader = body.source()
            InAppLogger.d("ApiService", "handleStreamingResponse: Starting to read SSE stream")
            val eventDataLines = mutableListOf<String>()
            var readingDataEvent = false
             
            while (!reader.exhausted()) {
                val line = reader.readUtf8Line() ?: break

                if (line.isBlank()) {
                    val data = eventDataLines.joinToString(separator = "\n")
                    eventDataLines.clear()
                    if (data.isBlank()) continue

                    val handled = handleSseData(data, onChunk, onParsedChunk = { chunkCount++ })
                    if (handled == SseHandleResult.Done) break
                    if (handled == SseHandleResult.ParseError) parseErrorCount++
                    readingDataEvent = false
                    continue
                }

                if (line.startsWith("data:")) {
                    readingDataEvent = true
                    eventDataLines += line.removePrefix("data:").trimStart()
                } else if (readingDataEvent && !line.startsWith("event:") && !line.startsWith("id:")) {
                    eventDataLines += line
                }
            }

            if (eventDataLines.isNotEmpty()) {
                val data = eventDataLines.joinToString(separator = "\n")
                val handled = handleSseData(data, onChunk, onParsedChunk = { chunkCount++ })
                if (handled == SseHandleResult.ParseError) parseErrorCount++
            }
             
            InAppLogger.d("ApiService", "handleStreamingResponse: Stream ended. Chunks=$chunkCount, ParseErrors=$parseErrorCount")
        } ?: run {
            InAppLogger.e("ApiService", "handleStreamingResponse: Response body is null")
        }
    }

    private fun handleSseData(
        data: String,
        onChunk: (String, Boolean) -> Unit,
        onParsedChunk: () -> Unit
    ): SseHandleResult {
        if (data == "[DONE]") {
            InAppLogger.d("ApiService", "handleStreamingResponse: Received [DONE]")
            onChunk("", true)
            return SseHandleResult.Done
        }

        return try {
            val chunkResponse = json.decodeFromString<StreamChatChunk>(data)
            val content = chunkResponse.choices.firstOrNull()?.delta?.content ?: ""

            if (content.isNotEmpty()) {
                onParsedChunk()
                onChunk(content, false)
            }
            SseHandleResult.Parsed
        } catch (e: Exception) {
            InAppLogger.e("ApiService", "handleStreamingResponse: Failed to parse event: ${e.message}, data: $data")
            SseHandleResult.ParseError
        }
    }

    private enum class SseHandleResult {
        Parsed,
        ParseError,
        Done
    }
}
