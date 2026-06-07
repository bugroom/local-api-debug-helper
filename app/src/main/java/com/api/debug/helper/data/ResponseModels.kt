package com.api.debug.helper.data

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val uptime: Long,
    val connections: Int,
    val loaded_model: String? = null
)

@Serializable
data class ModelsResponse(
    val `object`: String = "list",
    val data: List<Model>
)

@Serializable
data class Model(
    val id: String,
    val `object`: String = "model",
    val owned_by: String = "local",
    val created: Long = 0
)
