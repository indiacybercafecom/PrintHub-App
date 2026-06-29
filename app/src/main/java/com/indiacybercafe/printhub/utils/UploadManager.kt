package com.indiacybercafe.printhub.utils

import android.content.Context
import com.indiacybercafe.printhub.models.OrderDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UploadStatus {
    IDLE, UPLOADING, COMPLETED, FAILED
}

data class UploadState(
    val uploadId: String = "",
    val currentFileName: String = "",
    val totalFiles: Int = 0,
    val uploadedFiles: Int = 0,
    val totalBytes: Long = 0,
    val uploadedBytes: Long = 0,
    val progressPercentage: Int = 0,
    val uploadSpeed: String = "0 KB/s",
    val estimatedTime: String = "--",
    val status: UploadStatus = UploadStatus.IDLE,
    val orderDraft: OrderDraft? = null,
    val isVisible: Boolean = false
)

object UploadManager {
    private const val PREF_NAME = "upload_prefs"
    
    interface UploadListener {
        fun onStateChanged(state: UploadState)
    }

    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val listeners = mutableListOf<UploadListener>()

    private var lastUpdateTime: Long = 0
    private var lastBytesTransferred: Long = 0

    @JvmStatic
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val uploadId = prefs.getString("uploadId", "") ?: ""
        if (uploadId.isNotEmpty()) {
            val statusStr = prefs.getString("status", UploadStatus.IDLE.name)
            val status = try { UploadStatus.valueOf(statusStr ?: UploadStatus.IDLE.name) } catch (e: Exception) { UploadStatus.IDLE }
            
            _uploadState.value = UploadState(
                uploadId = uploadId,
                status = if (status == UploadStatus.UPLOADING) UploadStatus.FAILED else status,
                totalFiles = prefs.getInt("totalFiles", 0),
                uploadedFiles = prefs.getInt("uploadedFiles", 0),
                progressPercentage = prefs.getInt("progressPercentage", 0),
                isVisible = prefs.getBoolean("isVisible", false)
            )
        }
    }

    private fun saveState(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val state = _uploadState.value
        prefs.edit().apply {
            putString("uploadId", state.uploadId)
            putString("status", state.status.name)
            putInt("totalFiles", state.totalFiles)
            putInt("uploadedFiles", state.uploadedFiles)
            putInt("progressPercentage", state.progressPercentage)
            putBoolean("isVisible", state.isVisible)
            apply()
        }
    }

    @JvmStatic
    fun getCurrentState(): UploadState = _uploadState.value

    @JvmStatic
    fun addListener(listener: UploadListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            listener.onStateChanged(_uploadState.value)
        }
    }

    @JvmStatic
    fun removeListener(listener: UploadListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val state = _uploadState.value
        listeners.forEach { it.onStateChanged(state) }
    }

    @JvmStatic
    fun startUpload(context: Context, orderId: String, totalFiles: Int, totalBytes: Long, orderDraft: OrderDraft) {
        _uploadState.value = UploadState(
            uploadId = orderId,
            totalFiles = totalFiles,
            totalBytes = totalBytes,
            orderDraft = orderDraft,
            status = UploadStatus.UPLOADING,
            isVisible = true
        )
        lastUpdateTime = System.currentTimeMillis()
        lastBytesTransferred = 0
        saveState(context)
        notifyListeners()
    }

    @JvmStatic
    fun updateProgress(
        context: Context,
        currentFileName: String,
        uploadedFiles: Int,
        totalBytesTransferred: Long
    ) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastUpdateTime
        
        var speedStr = _uploadState.value.uploadSpeed
        var etaStr = _uploadState.value.estimatedTime

        if (timeDiff >= 1000) {
            val bytesDiff = totalBytesTransferred - lastBytesTransferred
            val speed = (bytesDiff * 1000) / timeDiff
            
            speedStr = formatSpeed(speed)
            
            val remainingBytes = _uploadState.value.totalBytes - totalBytesTransferred
            if (speed > 0) {
                val etaSeconds = remainingBytes / speed
                etaStr = formatTime(etaSeconds)
            }
            
            lastUpdateTime = currentTime
            lastBytesTransferred = totalBytesTransferred
        }

        val percentage = if (_uploadState.value.totalBytes > 0) {
            ((100.0 * totalBytesTransferred) / _uploadState.value.totalBytes).toInt()
        } else 0

        _uploadState.value = _uploadState.value.copy(
            currentFileName = currentFileName,
            uploadedFiles = uploadedFiles,
            uploadedBytes = totalBytesTransferred,
            progressPercentage = percentage,
            uploadSpeed = speedStr,
            estimatedTime = etaStr
        )
        
        if (System.currentTimeMillis() % 10 == 0L) {
            saveState(context)
        }
        
        notifyListeners()
    }

    @JvmStatic
    fun completeUpload(context: Context) {
        _uploadState.value = _uploadState.value.copy(
            status = UploadStatus.COMPLETED,
            progressPercentage = 100,
            uploadSpeed = "0 KB/s",
            estimatedTime = "00:00"
        )
        saveState(context)
        notifyListeners()
    }

    @JvmStatic
    fun setFailed(context: Context) {
        _uploadState.value = _uploadState.value.copy(
            status = UploadStatus.FAILED,
            isVisible = true
        )
        saveState(context)
        notifyListeners()
    }

    @JvmStatic
    fun dismissWidget(context: Context) {
        _uploadState.value = _uploadState.value.copy(isVisible = false)
        saveState(context)
        notifyListeners()
    }

    @JvmStatic
    fun reset(context: Context) {
        _uploadState.value = UploadState()
        saveState(context)
        notifyListeners()
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0))
            bytesPerSecond >= 1024 -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
            else -> "$bytesPerSecond B/s"
        }
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
