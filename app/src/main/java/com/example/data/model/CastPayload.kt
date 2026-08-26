package com.example.data.model

enum class MediaType {
    VIDEO,
    AUDIO,
    PHOTO,
    WEB_STREAM,
    YOUTUBE,
    PDF_DOC,
    WEB_PAGE
}

data class CastPayload(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val subtitle: String = "",
    val mediaType: MediaType = MediaType.VIDEO,
    val streamUrl: String,
    val mimeType: String = "video/mp4",
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val thumbnailUrl: String = "",
    val isLiveStream: Boolean = false
)
