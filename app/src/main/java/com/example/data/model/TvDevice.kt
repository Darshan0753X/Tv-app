package com.example.data.model

data class TvDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8080,
    val model: String = "Android TV Box",
    val isPaired: Boolean = false,
    val isConnected: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val pin: String = "1234",
    val signalStrength: Int = 95, // 0..100%
    val osVersion: String = "Android TV 14 / Set-Top Box",
    val isReceiverModeActive: Boolean = false
)
