package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_devices")
data class TvDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8080,
    val model: String = "Android TV Box",
    val isPaired: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val pin: String = "1234",
    val signalStrength: Int = 90
)

@Entity(tableName = "shared_files")
data class SharedFileEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val fileSize: Long,
    val formattedSize: String,
    val category: String, // VIDEO, AUDIO, PDF, APK, etc.
    val localUriOrPath: String,
    val downloadUrl: String,
    val isIncoming: Boolean,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_bookmarks")
data class BrowserBookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val faviconLetter: String = "W",
    val category: String = "General"
)

@Entity(tableName = "cast_history")
data class CastHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val mediaType: String,
    val streamUrl: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
