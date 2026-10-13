package com.example.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.data.model.DiscoveryProtocol
import com.example.data.model.TvDevice
import com.example.network.NetworkUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class NsdServiceFilter(val label: String, val serviceType: String?) {
    ALL("All Services", null),
    ANDROID_TV("Android TV Remote", "_androidtv-remote._tcp."),
    GOOGLE_CAST("Google Cast", "_googlecast._tcp."),
    TV_HUB("TV Hub Companion", "_tvhub._tcp."),
    AIRPLAY("AirPlay", "_airplay._tcp.")
}

data class NsdLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val serviceName: String,
    val serviceType: String,
    val details: String
)

class TvNsdDiscoveryService(private val context: Context) {

    companion object {
        const val TAG = "TvNsdDiscovery"

        // Standard Android TV & Cast service types
        val SUPPORTED_SERVICE_TYPES = listOf(
            "_androidtv-remote._tcp.",
            "_googlecast._tcp.",
            "_tvhub._tcp.",
            "_airplay._tcp."
        )
    }

    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null

    // State flows
    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _activeFilter = MutableStateFlow(NsdServiceFilter.ALL)
    val activeFilter: StateFlow<NsdServiceFilter> = _activeFilter.asStateFlow()

    private val _eventLogs = MutableStateFlow<List<NsdLogEntry>>(emptyList())
    val eventLogs: StateFlow<List<NsdLogEntry>> = _eventLogs.asStateFlow()

    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val _broadcastServiceName = MutableStateFlow<String?>(null)
    val broadcastServiceName: StateFlow<String?> = _broadcastServiceName.asStateFlow()

    // Tracking active listeners by service type
    private val activeListeners = ConcurrentHashMap<String, NsdManager.DiscoveryListener>()
    private var registrationListener: NsdManager.RegistrationListener? = null

    // Serialized resolve worker
    private val resolveQueue = Channel<NsdServiceInfo>(capacity = Channel.UNLIMITED)
    private var resolveWorkerJob: Job? = null
    private var scanScope: CoroutineScope? = null

    init {
        // Prepare multicast lock
        try {
            multicastLock = wifiManager?.createMulticastLock("TvNsdDiscoveryLock")?.apply {
                setReferenceCounted(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Multicast lock initialization warning: ${e.message}")
        }
    }

    fun setFilter(filter: NsdServiceFilter) {
        _activeFilter.value = filter
    }

    /**
     * Start discovery using NsdManager on local Wi-Fi
     */
    fun startDiscovery(scope: CoroutineScope) {
        if (_isScanning.value) return
        _isScanning.value = true
        scanScope = scope

        acquireMulticastLock()
        logEvent("SCAN_STARTED", "*", "mDNS", "Initiated NsdManager local network discovery")

        // Start serial resolver worker to prevent NsdManager FAILURE_ALREADY_ACTIVE
        startResolveWorker(scope)

        val typesToDiscover = when (val filter = _activeFilter.value) {
            NsdServiceFilter.ALL -> SUPPORTED_SERVICE_TYPES
            else -> listOfNotNull(filter.serviceType)
        }

        for (serviceType in typesToDiscover) {
            registerDiscoveryForType(serviceType)
        }
    }

    private fun registerDiscoveryForType(serviceType: String) {
        if (nsdManager == null) {
            logEvent("ERROR", "NsdManager", serviceType, "NsdManager not available on this device")
            return
        }

        // Avoid duplicate listener for same type
        if (activeListeners.containsKey(serviceType)) return

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started for $regType")
                logEvent("DISCOVERY_STARTED", "*", regType, "Listening for local service beacons")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val sName = serviceInfo.serviceName ?: "Unknown"
                val sType = serviceInfo.serviceType ?: serviceType
                Log.d(TAG, "Found service: $sName ($sType)")
                logEvent("SERVICE_FOUND", sName, sType, "Discovered on local Wi-Fi, queuing resolve")
                // Queue resolution sequentially
                resolveQueue.trySend(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val sName = serviceInfo.serviceName ?: "Unknown"
                val sType = serviceInfo.serviceType ?: serviceType
                Log.d(TAG, "Service lost: $sName ($sType)")
                logEvent("SERVICE_LOST", sName, sType, "Service offline or disconnected")
                removeDeviceByServiceName(sName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped for $serviceType")
                logEvent("DISCOVERY_STOPPED", "*", serviceType, "Discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed for $serviceType: code $errorCode")
                logEvent("ERROR", "*", serviceType, "Failed to start discovery (code $errorCode)")
                activeListeners.remove(serviceType)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed for $serviceType: code $errorCode")
                activeListeners.remove(serviceType)
            }
        }

        try {
            activeListeners[serviceType] = listener
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting discovery for $serviceType: ${e.message}")
            logEvent("ERROR", "*", serviceType, "Exception: ${e.message}")
            activeListeners.remove(serviceType)
        }
    }

    /**
     * Sequential resolve worker ensuring only 1 service is resolved at a time
     */
    private fun startResolveWorker(scope: CoroutineScope) {
        resolveWorkerJob?.cancel()
        resolveWorkerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val serviceInfo = resolveQueue.receive()
                if (!isActive) break
                resolveServiceInfoWithTimeout(serviceInfo)
            }
        }
    }

    private suspend fun resolveServiceInfoWithTimeout(serviceInfo: NsdServiceInfo) {
        val sName = serviceInfo.serviceName ?: "Unknown"
        val sType = serviceInfo.serviceType ?: "Unknown"

        logEvent("RESOLVING", sName, sType, "Resolving host IP, port, and TXT attributes...")

        val deferred = CompletableDeferred<NsdServiceInfo?>()

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: error $errorCode")
                deferred.complete(null)
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                Log.d(TAG, "Resolved service: ${resolvedInfo?.serviceName} at ${resolvedInfo?.host?.hostAddress}:${resolvedInfo?.port}")
                deferred.complete(resolvedInfo)
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
            // 3.5s timeout per resolve
            val resolved = withTimeoutOrNull(3500L) {
                deferred.await()
            }

            if (resolved != null) {
                handleResolvedService(resolved)
            } else {
                logEvent("RESOLVE_FAILED", sName, sType, "Timeout or resolve failure")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving $sName: ${e.message}")
            logEvent("RESOLVE_ERROR", sName, sType, "Error: ${e.message}")
        }
    }

    private fun handleResolvedService(info: NsdServiceInfo) {
        val hostAddress = info.host?.hostAddress ?: return
        val port = info.port
        val rawName = info.serviceName ?: "Android TV"
        val serviceType = info.serviceType ?: "_tvhub._tcp."

        // Extract TXT record attributes
        val attributes = mutableMapOf<String, String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                info.attributes?.forEach { (key, valueBytes) ->
                    if (valueBytes != null) {
                        attributes[key] = String(valueBytes, Charsets.UTF_8)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Attributes parse error: ${e.message}")
            }
        }

        // Derive user-friendly device name
        val friendlyName = attributes["fn"]
            ?: attributes["name"]
            ?: parseFriendlyNameFromService(rawName)

        // Derive model
        val model = attributes["md"]
            ?: attributes["model"]
            ?: deriveModelFromServiceType(serviceType)

        val device = TvDevice(
            id = "nsd_${hostAddress.replace(".", "_")}_$port",
            name = friendlyName,
            ipAddress = hostAddress,
            port = if (port > 0) port else 8080,
            model = model,
            pin = attributes["pin"] ?: "",
            signalStrength = 95,
            discoveryProtocol = DiscoveryProtocol.NSD,
            serviceType = serviceType,
            serviceName = rawName,
            attributes = attributes,
            lastSeenTimestamp = System.currentTimeMillis()
        )

        addOrUpdateDevice(device)
        logEvent(
            "SERVICE_RESOLVED",
            friendlyName,
            serviceType,
            "Resolved to $hostAddress:$port ($model)"
        )
    }

    private fun parseFriendlyNameFromService(rawName: String): String {
        return rawName.replace(Regex("-[0-9a-fA-F]{4,}$"), "")
            .replace("_", " ")
            .trim()
            .ifEmpty { "Android TV" }
    }

    private fun deriveModelFromServiceType(type: String): String {
        return when {
            type.contains("googlecast", ignoreCase = true) -> "Google Cast / Android TV"
            type.contains("androidtv", ignoreCase = true) -> "Android TV Remote"
            type.contains("airplay", ignoreCase = true) -> "AirPlay Receiver"
            type.contains("tvhub", ignoreCase = true) -> "Android TV Hub Companion"
            else -> "Smart TV / Set-Top Box"
        }
    }

    fun addOrUpdateDevice(device: TvDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst {
            it.ipAddress == device.ipAddress && (it.port == device.port || it.serviceType == device.serviceType)
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

    private fun removeDeviceByServiceName(serviceName: String) {
        val current = _discoveredDevices.value.filterNot { it.serviceName == serviceName }
        _discoveredDevices.value = current
    }

    /**
     * Register this device as an Android TV receiver on the local Wi-Fi network using NsdManager
     */
    fun registerTvReceiverService(tvName: String, port: Int = 8080) {
        if (_isBroadcasting.value) return
        if (nsdManager == null) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = tvName
            serviceType = "_tvhub._tcp."
            setPort(port)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAttribute("name", tvName)
                setAttribute("model", Build.MODEL ?: "Android TV")
                setAttribute("version", "1.0")
                setAttribute("authRequired", "true")
                setAttribute("fn", tvName)
            }
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredInfo: NsdServiceInfo) {
                val name = registeredInfo.serviceName
                Log.i(TAG, "TV Receiver registered on NSD as: $name")
                _isBroadcasting.value = true
                _broadcastServiceName.value = name
                logEvent("SERVICE_REGISTERED", name, "_tvhub._tcp.", "Broadcasting on Wi-Fi port $port")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD registration failed: code $errorCode")
                _isBroadcasting.value = false
                _broadcastServiceName.value = null
                logEvent("ERROR", info.serviceName ?: "TV", "_tvhub._tcp.", "Registration failed: code $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.i(TAG, "NSD service unregistered")
                _isBroadcasting.value = false
                _broadcastServiceName.value = null
                logEvent("UNREGISTERED", info.serviceName ?: "TV", "_tvhub._tcp.", "Broadcast stopped")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD unregistration failed: code $errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call registerService: ${e.message}")
            logEvent("ERROR", tvName, "_tvhub._tcp.", "Exception: ${e.message}")
        }
    }

    fun unregisterTvReceiverService() {
        if (!_isBroadcasting.value) return
        val listener = registrationListener ?: return
        try {
            nsdManager?.unregisterService(listener)
        } catch (e: Exception) {
            Log.d(TAG, "Error unregistering NSD service: ${e.message}")
        } finally {
            _isBroadcasting.value = false
            _broadcastServiceName.value = null
            registrationListener = null
        }
    }

    /**
     * Stop NSD scan and clean up listeners
     */
    fun stopDiscovery() {
        if (!_isScanning.value) return
        _isScanning.value = false

        activeListeners.forEach { (sType, listener) ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.d(TAG, "Error stopping discovery for $sType: ${e.message}")
            }
        }
        activeListeners.clear()

        resolveWorkerJob?.cancel()
        releaseMulticastLock()
        logEvent("SCAN_STOPPED", "*", "mDNS", "NsdManager discovery stopped")
    }

    /**
     * Injects realistic simulated local Android TV discovery results (useful when no physical TV is on network)
     */
    fun simulateDiscoveredTv(presetType: NsdServiceFilter = NsdServiceFilter.ALL) {
        val localIp = NetworkUtils.getLocalIpAddress(context).ifBlank { "192.168.1.100" }
        val subnet = localIp.substringBeforeLast(".") + "."

        val samples = listOf(
            TvDevice(
                id = "nsd_sim_google_tv",
                name = "Living Room Sony BRAVIA 4K",
                ipAddress = "${subnet}105",
                port = 8009,
                model = "Sony BRAVIA (Google TV)",
                pin = "4821",
                signalStrength = 98,
                discoveryProtocol = DiscoveryProtocol.NSD,
                serviceType = "_googlecast._tcp.",
                serviceName = "Sony-BRAVIA-4K-8472",
                attributes = mapOf(
                    "fn" to "Living Room Sony BRAVIA 4K",
                    "md" to "Sony BRAVIA 4K XR-65A80J",
                    "id" to "sony_bravia_living_room",
                    "rm" to "Google TV 14",
                    "ve" to "05"
                )
            ),
            TvDevice(
                id = "nsd_sim_android_remote",
                name = "Bedroom NVIDIA SHIELD TV Pro",
                ipAddress = "${subnet}112",
                port = 6466,
                model = "NVIDIA SHIELD TV Pro",
                pin = "1234",
                signalStrength = 92,
                discoveryProtocol = DiscoveryProtocol.NSD,
                serviceType = "_androidtv-remote._tcp.",
                serviceName = "SHIELD-AndroidTV-Remote",
                attributes = mapOf(
                    "fn" to "Bedroom NVIDIA SHIELD TV Pro",
                    "md" to "NVIDIA SHIELD Android TV",
                    "version" to "2.1",
                    "authRequired" to "true"
                )
            ),
            TvDevice(
                id = "nsd_sim_tv_hub",
                name = "Office TCL Android TV Hub",
                ipAddress = "${subnet}120",
                port = 8080,
                model = "TCL 55Q650G (Android TV 14)",
                pin = "9940",
                signalStrength = 88,
                discoveryProtocol = DiscoveryProtocol.NSD,
                serviceType = "_tvhub._tcp.",
                serviceName = "TCL-TV-Hub-Companion",
                attributes = mapOf(
                    "name" to "Office TCL Android TV Hub",
                    "model" to "TCL 55Q650G",
                    "version" to "1.0",
                    "authRequired" to "true"
                )
            )
        )

        val filtered = when (presetType) {
            NsdServiceFilter.GOOGLE_CAST -> samples.filter { it.serviceType.contains("googlecast") }
            NsdServiceFilter.ANDROID_TV -> samples.filter { it.serviceType.contains("androidtv") }
            NsdServiceFilter.TV_HUB -> samples.filter { it.serviceType.contains("tvhub") }
            else -> samples
        }

        filtered.forEach { device ->
            addOrUpdateDevice(device)
            logEvent(
                "SERVICE_RESOLVED",
                device.name,
                device.serviceType,
                "Simulated local Wi-Fi discovery at ${device.ipAddress}:${device.port}"
            )
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock?.isHeld != true) {
                multicastLock?.acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not release multicast lock: ${e.message}")
        }
    }

    private fun logEvent(eventType: String, serviceName: String, serviceType: String, details: String) {
        val entry = NsdLogEntry(
            eventType = eventType,
            serviceName = serviceName,
            serviceType = serviceType,
            details = details
        )
        val current = _eventLogs.value.toMutableList()
        current.add(0, entry) // Prepend newest first
        if (current.size > 50) {
            current.removeAt(current.lastIndex)
        }
        _eventLogs.value = current
    }

    fun clearLogs() {
        _eventLogs.value = emptyList()
    }
}
