package com.api.debug.helper.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

 data class LogEntry(
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun formatted(): String {
        return "${formattedTime()} [${level}] ${tag}: ${message}"
    }
}

 object InAppLogger {
    private const val MAX_LOGS = 500
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val _logFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logFlow: StateFlow<List<LogEntry>> = _logFlow.asStateFlow()
    
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("D", tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("I", tag, message)
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog("W", tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "${message}\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        Log.e(tag, message, throwable)
        addLog("E", tag, fullMessage)
    }
    
    fun v(tag: String, message: String) {
        Log.v(tag, message)
        addLog("V", tag, message)
    }
    
    private fun addLog(level: String, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )
        logs.add(entry)
        
        // Remove old logs if exceeding limit
        while (logs.size > MAX_LOGS) {
            logs.poll()
        }
        
        _logFlow.value = logs.toList()
    }
    
    fun clear() {
        logs.clear()
        _logFlow.value = emptyList()
    }
    
    fun getAllLogs(): String {
        return logs.joinToString("\n") { it.formatted() }
    }
}