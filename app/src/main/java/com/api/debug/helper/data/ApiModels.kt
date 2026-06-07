package com.api.debug.helper.data

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String = ""
)

@Serializable
data class ChatCompletionRequest(
    val model: String = "",
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val top_p: Double? = null,
    val top_k: Int? = null,
    val stream: Boolean = false
)

@Serializable
data class ChatCompletionResponse(
    val id: String = "chatcmpl-local",
    val `object`: String = "chat.completion",
    val created: Long = 0,
    val model: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: ChatMessage = ChatMessage(role = "assistant"),
    val finish_reason: String? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

// Streaming response models
@Serializable
data class StreamChatChunk(
    val id: String = "chatcmpl-local",
    val `object`: String = "chat.completion.chunk",
    val created: Long = 0,
    val model: String = "",
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta = StreamDelta(),
    val finish_reason: String? = null
)

@Serializable
data class StreamDelta(
    val content: String = "",
    val role: String? = null
)
