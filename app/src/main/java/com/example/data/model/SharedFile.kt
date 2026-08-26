package com.example.data.model

enum class FileCategory {
    VIDEO,
    AUDIO,
    PDF,
    APK,
    IMAGE,
    DOCUMENT,
    ARCHIVE,
    OTHER
}

enum class TransferStatus {
    IDLE,
    PREPARING,
    TRANSFERRING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class SharedFile(
    val id: String = System.currentTimeMillis().toString(),
    val fileName: String,
    val fileSize: Long,
    val formattedSize: String,
    val category: FileCategory = FileCategory.VIDEO,
    val localUriOrPath: String = "",
    val downloadUrl: String = "",
    val progress: Float = 0f, // 0.0 to 1.0
    val speedMbps: Double = 0.0,
    val isIncoming: Boolean = false,
    val status: TransferStatus = TransferStatus.IDLE,
    val timestamp: Long = System.currentTimeMillis(),
    val targetDeviceId: String = ""
)
