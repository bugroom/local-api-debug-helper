package com.api.debug.helper.data

data class ServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val apiKey: String = ""
) {
    val baseUrl: String
        get() = "http://$host:$port"
}