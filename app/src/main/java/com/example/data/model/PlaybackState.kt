package com.example.data.model

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = "Ready to Play",
    val subtitle: String = "No media casted yet",
    val mediaType: MediaType = MediaType.VIDEO,
    val streamUrl: String = "",
    val volume: Int = 75, // 0..100
    val isMuted: Boolean = false,
    val isBuffering: Boolean = false,
    val aspectRatio: String = "16:9",
    val activePage: Int = 1,
    val totalPages: Int = 1
)
