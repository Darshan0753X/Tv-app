package com.example.data.model

import android.graphics.Bitmap

enum class MirrorSource(val title: String, val subtitle: String, val iconName: String) {
    FULL_SCREEN("Phone Display (MediaProjection)", "Complete live device screen & apps", "phone_android"),
    PHOTOS_GALLERY("Photo & 4K Video Gallery", "Mirror high-res photos & camera roll", "photo_library"),
    CAMERA_LIVE("Live Camera Feed", "Stream real-time phone camera to TV", "photo_camera"),
    GAME_MODE("Gaming Display (60 FPS)", "Ultra-low latency gaming view", "sports_esports"),
    DOCUMENT_SLIDES("Presentation & Docs", "Documents, spreadsheets & notes", "description")
}

enum class MirrorOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class MirrorQuality(val label: String, val resolution: String, val width: Int, val height: Int, val fps: Int, val bitrateMbps: Float) {
    BALANCED("1080p Standard", "1920x1080", 1080, 1920, 30, 8.5f),
    ULTRA_HD("1080p 60fps Pro", "1920x1080", 1080, 1920, 60, 16.0f),
    LOW_LATENCY("720p Ultra Fast", "1280x720", 720, 1280, 60, 6.0f),
    CINEMA_4K("4K Ultra High-Bitrate", "3840x2160", 2160, 3840, 60, 28.0f)
}

data class ScreenMirrorState(
    val isMirroring: Boolean = false,
    val isMediaProjectionActive: Boolean = false,
    val source: MirrorSource = MirrorSource.FULL_SCREEN,
    val quality: MirrorQuality = MirrorQuality.ULTRA_HD,
    val orientation: MirrorOrientation = MirrorOrientation.PORTRAIT,
    val isAudioEnabled: Boolean = true,
    val isMicEnabled: Boolean = false,
    val isPrivacyMode: Boolean = false,
    val latencyMs: Int = 12,
    val activeFps: Int = 60,
    val currentBitrateMbps: Float = 15.4f,
    val capturedFramesCount: Long = 0L,
    val streamUrl: String = "",
    val liveBitmap: Bitmap? = null,
    val activeAppTitle: String = "Home Screen",
    val activePhotoIndex: Int = 0,
    val connectedDeviceName: String = "Android TV 4K",
    val startTime: Long = 0L,
    val annotationLines: List<AnnotationPoint> = emptyList()
)

data class AnnotationPoint(
    val x: Float,
    val y: Float,
    val colorHex: Long = 0xFFFF0055,
    val strokeWidth: Float = 6f
)
