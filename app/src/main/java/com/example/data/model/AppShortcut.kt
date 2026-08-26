package com.example.data.model

data class AppShortcut(
    val id: String,
    val name: String,
    val packageName: String,
    val iconName: String, // e.g. "youtube", "netflix", "browser", "pdf", "files", "music", "prime", "gallery"
    val defaultUrl: String = "",
    val description: String = "",
    val isPreinstalled: Boolean = true
)

data class BrowserBookmark(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val url: String,
    val faviconLetter: String = "W",
    val category: String = "General"
)
