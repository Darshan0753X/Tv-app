package com.example.network.discovery

import android.content.Context
import android.util.Log
import com.example.data.model.DiscoveryProtocol
import com.example.data.model.TvDevice
import com.example.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL

class TvDiscoveryService(private val context: Context) {

    val nsdDiscovery = TvNsdDiscoveryService(context)

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null
    private var udpListenJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var nsdSyncJob: Job? = null

    companion object {
        const val UDP_PORT = 8889
        const val HTTP_PORT = 8080
        const val DISCOVERY_TAG = "TvDiscovery"
        const val DISCOVERY_BEACON_PREFIX = "TV_HUB_BEACON|"
        const val DISCOVERY_QUERY = "TV_HUB_DISCOVER_PING"
    }

    /**
     * Start listening for TV beacon broadcasts on UDP 8889 & sync NSD discoveries
     */
    fun startListeningForBeacons(scope: CoroutineScope) {
        // Observe NSD discovered devices and sync into master list
        nsdSyncJob?.cancel()
        nsdSyncJob = scope.launch(Dispatchers.Main) {
            nsdDiscovery.discoveredDevices.collect { nsdList ->
                nsdList.forEach { addOrUpdateDevice(it) }
            }
        }

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
                        // Format: TV_HUB_BEACON|Name|Port|Model|AUTH_REQUIRED
                        val parts = message.split("|")
                        if (parts.size >= 4) {
                            val name = parts[1]
                            val port = parts[2].toIntOrNull() ?: HTTP_PORT
                            val model = parts[3]

                            val device = TvDevice(
                                id = "tv_${senderIp.replace(".", "_")}",
                                name = name,
                                ipAddress = senderIp,
                                port = port,
                                model = model,
                                pin = "",
                                signalStrength = 95,
                                discoveryProtocol = DiscoveryProtocol.UDP_BEACON,
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
     * Broadcast a discovery query to the subnet, start NsdManager mDNS discovery, and probe IPs
     */
    fun startDiscoveryScan(scope: CoroutineScope) {
        scanJob?.cancel()
        _isScanning.value = true

        // 1. Start NsdManager discovery on local Wi-Fi
        nsdDiscovery.startDiscovery(scope)

        scanJob = scope.launch(Dispatchers.IO) {
            try {
                // 2. Send UDP broadcast ping
                sendUdpBroadcastPing()

                // 3. Scan local subnet IP range for HTTP server response on port 8080
                val localIp = NetworkUtils.getLocalIpAddress(context)
                if (localIp.isBlank() || localIp == "127.0.0.1") {
                    return@launch
                }
                val subnetPrefix = localIp.substringBeforeLast(".") + "."

                val currentLastOctet = localIp.substringAfterLast(".").toIntOrNull() ?: 100
                val ipsToScan = mutableListOf<String>()

                for (i in 1..254) {
                    if (i != currentLastOctet) {
                        ipsToScan.add("$subnetPrefix$i")
                    }
                }

                // Chunked parallel probe with awaitAll to maintain bounded concurrency
                coroutineScope {
                    ipsToScan.chunked(25).forEach { chunk ->
                        if (!isActive) return@coroutineScope
                        chunk.map { ip ->
                            async(Dispatchers.IO) {
                                probeDeviceAtIp(ip)
                            }
                        }.awaitAll()
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

    /**
     * Probes an IP and validates with GET /api/info to confirm it is genuinely a TV Hub receiver
     */
    private fun probeDeviceAtIp(ip: String) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("http://$ip:$HTTP_PORT/api/info")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 500
                readTimeout = 750
                requestMethod = "GET"
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val name = json.optString("name", "Android TV Box")
                val model = json.optString("model", "Set-Top Box")

                val device = TvDevice(
                    id = "tv_${ip.replace(".", "_")}",
                    name = name,
                    ipAddress = ip,
                    port = HTTP_PORT,
                    model = model,
                    pin = "",
                    signalStrength = 90,
                    discoveryProtocol = DiscoveryProtocol.SUBNET_PROBE,
                    lastSeenTimestamp = System.currentTimeMillis()
                )
                addOrUpdateDevice(device)
            }
        } catch (e: Exception) {
            // Not a TV Hub device or unreachable
        } finally {
            connection?.disconnect()
        }
    }

    fun addOrUpdateDevice(device: TvDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst {
            it.ipAddress == device.ipAddress && (it.port == device.port || it.id == device.id)
        }
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
            pin = "",
            isPaired = true,
            isConnected = true,
            discoveryProtocol = DiscoveryProtocol.MANUAL
        )
        addOrUpdateDevice(device)
        return device
    }

    fun stop() {
        scanJob?.cancel()
        udpListenJob?.cancel()
        nsdSyncJob?.cancel()
        nsdDiscovery.stopDiscovery()
        try {
            udpSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}
