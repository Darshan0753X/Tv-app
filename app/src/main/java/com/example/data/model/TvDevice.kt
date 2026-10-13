package com.example.data.model

enum class DiscoveryProtocol {
    NSD,          // Network Service Discovery (mDNS / DNS-SD via NsdManager)
    UDP_BEACON,   // Direct UDP beacon
    SUBNET_PROBE, // Subnet scan probe
    MANUAL        // Manually added IP
}

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
    val isReceiverModeActive: Boolean = false,
    val discoveryProtocol: DiscoveryProtocol = DiscoveryProtocol.NSD,
    val serviceType: String = "",
    val serviceName: String = "",
    val attributes: Map<String, String> = emptyMap()
)
