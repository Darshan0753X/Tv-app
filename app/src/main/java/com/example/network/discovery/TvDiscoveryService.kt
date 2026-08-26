package com.example.network.discovery

import android.content.Context
import android.util.Log
import com.example.data.model.TvDevice
import com.example.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class TvDiscoveryService(private val context: Context) {

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null
    private var udpListenJob: Job? = null
    private var udpSocket: DatagramSocket? = null

    companion object {
        const val UDP_PORT = 8889
        const val HTTP_PORT = 8080
        const val DISCOVERY_TAG = "TvDiscovery"
        const val DISCOVERY_BEACON_PREFIX = "TV_HUB_BEACON|"
        const val DISCOVERY_QUERY = "TV_HUB_DISCOVER_PING"
    }

    /**
     * Start listening for TV beacon broadcasts on UDP 8889
     */
    fun startListeningForBeacons(scope: CoroutineScope) {
        udpListenJob?.cancel()
        udpListenJob = scope.launch(Dispatchers.IO) {
            try {
                udpSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_PORT))
                    broadcast = true
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    val senderIp = packet.address.hostAddress ?: continue

                    if (message.startsWith(DISCOVERY_BEACON_PREFIX)) {
                        // Format: TV_HUB_BEACON|Name|Port|Model|Pin
                        val parts = message.split("|")
                        if (parts.size >= 4) {
                            val name = parts[1]
                            val port = parts[2].toIntOrNull() ?: HTTP_PORT
                            val model = parts[3]
                            val pin = if (parts.size > 4) parts[4] else "1234"

                            val device = TvDevice(
                                id = "tv_${senderIp.replace(".", "_")}",
                                name = name,
                                ipAddress = senderIp,
                                port = port,
                                model = model,
                                pin = pin,
                                signalStrength = 95,
                                lastSeenTimestamp = System.currentTimeMillis()
                            )
                            addOrUpdateDevice(device)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(DISCOVERY_TAG, "UDP listener stopped or errored: ${e.message}")
            }
        }
    }

    /**
     * Broadcast a discovery query to the subnet and scan local IPs
     */
    fun startDiscoveryScan(scope: CoroutineScope) {
        scanJob?.cancel()
        _isScanning.value = true

        scanJob = scope.launch(Dispatchers.IO) {
            try {
                // 1. Send UDP broadcast ping
                sendUdpBroadcastPing()

                // 2. Scan local subnet IP range for HTTP server response on port 8080
                val localIp = NetworkUtils.getLocalIpAddress(context)
                val subnetPrefix = localIp.substringBeforeLast(".") + "."

                // Scan common range around current device (e.g. +-20 or 1..254)
                val currentLastOctet = localIp.substringAfterLast(".").toIntOrNull() ?: 100
                val ipsToScan = mutableListOf<String>()

                // Prioritize immediate neighbors
                for (i in 1..254) {
                    if (i != currentLastOctet) {
                        ipsToScan.add("$subnetPrefix$i")
                    }
                }

                // Chunked parallel probe
                ipsToScan.chunked(25).forEach { chunk ->
                    if (!isActive) return@launch
                    chunk.forEach { ip ->
                        launch(Dispatchers.IO) {
                            probeDeviceAtIp(ip)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(DISCOVERY_TAG, "Scan error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isScanning.value = false
                }
            }
        }
    }

    private fun sendUdpBroadcastPing() {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val data = DISCOVERY_QUERY.toByteArray()
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(data, data.size, broadcastAddress, UDP_PORT)
            socket.send(packet)
            socket.close()
        } catch (e: Exception) {
            Log.d(DISCOVERY_TAG, "UDP broadcast ping failed: ${e.message}")
        }
    }

    private fun probeDeviceAtIp(ip: String) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, HTTP_PORT), 400) // 400ms timeout
            socket.close()

            // Device is reachable on port 8080!
            val device = TvDevice(
                id = "tv_${ip.replace(".", "_")}",
                name = "Android TV ($ip)",
                ipAddress = ip,
                port = HTTP_PORT,
                model = "Android TV / Set-Top Box",
                pin = "1234",
                signalStrength = 90,
                lastSeenTimestamp = System.currentTimeMillis()
            )
            addOrUpdateDevice(device)
        } catch (e: Exception) {
            // Not responding or port closed
        }
    }

    fun addOrUpdateDevice(device: TvDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.ipAddress == device.ipAddress }
        if (index >= 0) {
            current[index] = device.copy(
                isPaired = current[index].isPaired,
                isConnected = current[index].isConnected
            )
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    fun addManualDevice(ip: String, port: Int, name: String): TvDevice {
        val device = TvDevice(
            id = "tv_manual_${ip.replace(".", "_")}",
            name = if (name.isNotBlank()) name else "TV Box ($ip)",
            ipAddress = ip,
            port = port,
            model = "Set-Top Box (Manual)",
            pin = "1234",
            isPaired = true,
            isConnected = true
        )
        addOrUpdateDevice(device)
        return device
    }

    fun stop() {
        scanJob?.cancel()
        udpListenJob?.cancel()
        try {
            udpSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}
