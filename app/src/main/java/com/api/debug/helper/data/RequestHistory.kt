package com.api.debug.helper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RequestHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val endpoint: String,
    val method: String,
    val requestBody: String,
    val responseBody: String,
    val statusCode: Int,
    val timestamp: Long = System.currentTimeMillis()
)