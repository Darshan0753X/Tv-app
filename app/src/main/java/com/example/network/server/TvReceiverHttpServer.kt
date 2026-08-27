package com.example.network.server

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.model.AppShortcut
import com.example.data.model.CastPayload
import com.example.data.model.CommandType
import com.example.data.model.FileCategory
import com.example.data.model.MediaType
import com.example.data.model.PlaybackState
import com.example.data.model.RemoteCommand
import com.example.data.model.SharedFile
import com.example.data.model.TransferStatus
import com.example.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TvReceiverHttpServer(private val context: Context) {

    companion object {
        const val SERVER_PORT = 8080
        const val UDP_BEACON_PORT = 8889
        const val TAG = "TvReceiverServer"
        const val MAX_UPLOAD_SIZE = 500L * 1024 * 1024 // 500 MB max file upload
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var beaconJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _tvName = MutableStateFlow("Living Room TV Box")
    val tvName: StateFlow<String> = _tvName.asStateFlow()

    private val _pinCode = MutableStateFlow("1234")
    val pinCode: StateFlow<String> = _pinCode.asStateFlow()

    // Valid authenticated session tokens
    private val authenticatedTokens = ConcurrentHashMap.newKeySet<String>()

    // Shared events dispatched to TV UI
    private val _incomingCommand = MutableSharedFlow<RemoteCommand>(extraBufferCapacity = 64)
    val incomingCommand: SharedFlow<RemoteCommand> = _incomingCommand.asSharedFlow()

    private val _incomingCast = MutableSharedFlow<CastPayload>(extraBufferCapacity = 16)
    val incomingCast: SharedFlow<CastPayload> = _incomingCast.asSharedFlow()

    private val _incomingFileTransfer = MutableSharedFlow<SharedFile>(extraBufferCapacity = 16)
    val incomingFileTransfer: SharedFlow<SharedFile> = _incomingFileTransfer.asSharedFlow()

    private val _incomingScreenFrame = MutableStateFlow<Bitmap?>(null)
    val incomingScreenFrame: StateFlow<Bitmap?> = _incomingScreenFrame.asStateFlow()

    private val _screenMirrorActive = MutableStateFlow(false)
    val screenMirrorActive: StateFlow<Boolean> = _screenMirrorActive.asStateFlow()

    private val _screenMirrorSourceTitle = MutableStateFlow("Phone Screen")
    val screenMirrorSourceTitle: StateFlow<String> = _screenMirrorSourceTitle.asStateFlow()

    private val _currentPlaybackState = MutableStateFlow(
        PlaybackState(
            isPlaying = false,
            title = "Android TV Ready",
            subtitle = "Connected on Wi-Fi - Ready for Cast & Remote",
            volume = 75
        )
    )
    val currentPlaybackState: StateFlow<PlaybackState> = _currentPlaybackState.asStateFlow()

    fun setTvName(name: String) {
        _tvName.value = name
    }

    fun setPinCode(pin: String) {
        _pinCode.value = pin
        authenticatedTokens.clear()
    }

    fun updatePlaybackState(state: PlaybackState) {
        _currentPlaybackState.value = state
    }

    /**
     * Start TV receiver Server on Port 8080 and UDP broadcast beacon on 8889
     */
    fun start(scope: CoroutineScope) {
        if (_isRunning.value) return
        _isRunning.value = true

        // 1. Start HTTP Server
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(SERVER_PORT))
                }
                Log.i(TAG, "TV Receiver HTTP Server started on port $SERVER_PORT")

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch(Dispatchers.IO) {
                            handleClientConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket error: ${e.message}")
            } finally {
                _isRunning.value = false
            }
        }

        // 2. Start UDP Beacon Broadcaster without leaking the PIN
        beaconJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val beaconMsg = "TV_HUB_BEACON|${_tvName.value}|$SERVER_PORT|Android TV Box|AUTH_REQUIRED"
                    val socket = DatagramSocket()
                    socket.broadcast = true
                    val data = beaconMsg.toByteArray()
                    val broadcastAddress = InetAddress.getByName("255.255.255.255")
                    val packet = DatagramPacket(data, data.size, broadcastAddress, UDP_BEACON_PORT)
                    socket.send(packet)
                    socket.close()
                } catch (e: Exception) {
                    Log.d(TAG, "Beacon broadcast tick error: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    /**
     * Parse HTTP headers cleanly from raw InputStream without BufferedReader buffering
     */
    private fun readHttpHeaders(input: InputStream): Pair<List<String>, Map<String, String>>? {
        val headerBytes = ByteArrayOutputStream()
        var matched = 0
        val maxHeaderBytes = 16 * 1024 // 16KB limit
        while (matched < 4 && headerBytes.size() < maxHeaderBytes) {
            val b = input.read()
            if (b == -1) return null
            headerBytes.write(b)
            if (matched == 0 && b == '\r'.code) matched = 1
            else if (matched == 1 && b == '\n'.code) matched = 2
            else if (matched == 2 && b == '\r'.code) matched = 3
            else if (matched == 3 && b == '\n'.code) matched = 4
            else if (b == '\r'.code) matched = 1
            else matched = 0
        }
        if (matched != 4) return null

        val headerText = headerBytes.toString(Charsets.US_ASCII.name())
        val lines = headerText.split("\r\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        val headers = mutableMapOf<String, String>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val k = line.substring(0, colonIdx).trim().lowercase()
                val v = line.substring(colonIdx + 1).trim()
                headers[k] = v
            }
        }
        return Pair(lines, headers)
    }

    private fun readExactBodyBytes(input: InputStream, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        val data = ByteArray(length)
        var total = 0
        while (total < length) {
            val read = input.read(data, total, length - total)
            if (read == -1) break
            total += read
        }
        return if (total == length) data else data.copyOf(total)
    }

    private fun isRequestAuthorized(headers: Map<String, String>, clientIp: String): Boolean {
        // Allow localhost loopback requests
        val localIp = NetworkUtils.getLocalIpAddress(context)
        if (clientIp == "127.0.0.1" || clientIp == "::1" || (localIp.isNotEmpty() && clientIp == localIp)) {
            return true
        }
        val authHeader = headers["authorization"] ?: ""
        if (authHeader.startsWith("Bearer ", ignoreCase = true)) {
            val token = authHeader.substring(7).trim()
            if (authenticatedTokens.contains(token)) return true
        }
        val pinHeader = headers["x-auth-pin"] ?: ""
        if (pinHeader.isNotBlank() && pinHeader == _pinCode.value) {
            return true
        }
        return false
    }

    private fun handleClientConnection(socket: Socket) {
        try {
            socket.soTimeout = 20000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val clientIp = socket.inetAddress?.hostAddress ?: "127.0.0.1"

            val parseResult = readHttpHeaders(input) ?: return
            val requestLines = parseResult.first
            val headers = parseResult.second

            val requestLine = requestLines.first()
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val pathWithQuery = parts[1]
            val path = pathWithQuery.substringBefore("?")
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0

            // Route handler
            when {
                path == "/api/info" && method == "GET" -> {
                    val localIp = NetworkUtils.getLocalIpAddress(context)
                    val json = JSONObject().apply {
                        put("name", _tvName.value)
                        put("ip", localIp)
                        put("port", SERVER_PORT)
                        put("model", "Android TV / Set-Top Box")
                        put("authRequired", true)
                        put("status", "ONLINE")
                        put("volume", _currentPlaybackState.value.volume)
                        put("isPlaying", _currentPlaybackState.value.isPlaying)
                        put("title", _currentPlaybackState.value.title)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                (path == "/api/pair" || path == "/api/auth") && method == "POST" -> {
                    val bodyBytes = readExactBodyBytes(input, contentLength)
                    val body = String(bodyBytes, Charsets.UTF_8)
                    val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
                    val providedPin = json.optString("pin", headers["x-auth-pin"] ?: "")
                    val clientName = json.optString("clientName", "Phone Remote Companion")

                    if (providedPin == _pinCode.value) {
                        val token = UUID.randomUUID().toString()
                        authenticatedTokens.add(token)
                        sendJsonResponse(output, 200, """{"status":"PAIRED","token":"$token","client":"$clientName"}""")
                    } else {
                        sendJsonResponse(output, 401, """{"status":"UNAUTHORIZED","error":"Invalid PIN"}""")
                    }
                }

                path == "/api/remote/command" && method == "POST" -> {
                    if (!isRequestAuthorized(headers, clientIp)) {
                        sendJsonResponse(output, 401, """{"error":"Unauthorized. Provide valid Bearer token or x-auth-pin"}""")
                        return
                    }

                    val bodyBytes = readExactBodyBytes(input, contentLength)
                    val body = String(bodyBytes, Charsets.UTF_8)
                    val json = JSONObject(body)
                    val cmdStr = json.optString("command", "DPAD_CENTER")
                    val cmdType = try { CommandType.valueOf(cmdStr) } catch (e: Exception) { CommandType.DPAD_CENTER }
                    val textVal = json.optString("textValue", "")
                    val intVal = json.optInt("intValue", 0)
                    val floatX = json.optDouble("floatX", 0.0).toFloat()
                    val floatY = json.optDouble("floatY", 0.0).toFloat()

                    val command = RemoteCommand(
                        commandType = cmdType,
                        textValue = textVal,
                        intValue = intVal,
                        floatX = floatX,
                        floatY = floatY
                    )

                    // Execute command side effects
                    when (cmdType) {
                        CommandType.VOLUME_UP -> {
                            val cur = _currentPlaybackState.value
                            _currentPlaybackState.value = cur.copy(volume = (cur.volume + 5).coerceAtMost(100))
                        }
                        CommandType.VOLUME_DOWN -> {
                            val cur = _currentPlaybackState.value
                            _currentPlaybackState.value = cur.copy(volume = (cur.volume - 5).coerceAtLeast(0))
                        }
                        CommandType.MUTE -> {
                            val cur = _currentPlaybackState.value
                            _currentPlaybackState.value = cur.copy(isMuted = !cur.isMuted)
                        }
                        CommandType.SET_VOLUME -> {
                            val cur = _currentPlaybackState.value
                            _currentPlaybackState.value = cur.copy(volume = intVal.coerceIn(0, 100))
                        }
                        CommandType.PLAY_PAUSE -> {
                            val cur = _currentPlaybackState.value
                            _currentPlaybackState.value = cur.copy(isPlaying = !cur.isPlaying)
                        }
                        CommandType.PLAY -> {
                            val cur = _currentPlaybackState.value
                            _currentPlaybackState.value = cur.copy(isPlaying = true)
                        }
                        CommandType.PAUSE -> {
                            val cur = _currentPlaybackState.value
                            _currentPlaybackState.value = cur.copy(isPlaying = false)
                        }
                        else -> {}
                    }

                    _incomingCommand.tryEmit(command)
                    sendJsonResponse(output, 200, """{"status":"OK","received":"$cmdStr"}""")
                }

                path == "/api/cast" && method == "POST" -> {
                    if (!isRequestAuthorized(headers, clientIp)) {
                        sendJsonResponse(output, 401, """{"error":"Unauthorized. Provide valid Bearer token or x-auth-pin"}""")
                        return
                    }

                    val bodyBytes = readExactBodyBytes(input, contentLength)
                    val body = String(bodyBytes, Charsets.UTF_8)
                    val json = JSONObject(body)
                    val title = json.optString("title", "Cast Media")
                    val subtitle = json.optString("subtitle", "")
                    val streamUrl = json.optString("streamUrl", "")
                    val mediaTypeStr = json.optString("mediaType", "VIDEO")
                    val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (e: Exception) { MediaType.VIDEO }
                    val durationMs = json.optLong("durationMs", 0L)

                    val payload = CastPayload(
                        title = title,
                        subtitle = subtitle,
                        streamUrl = streamUrl,
                        mediaType = mediaType,
                        durationMs = durationMs
                    )

                    _currentPlaybackState.value = _currentPlaybackState.value.copy(
                        isPlaying = true,
                        title = title,
                        subtitle = subtitle,
                        mediaType = mediaType,
                        streamUrl = streamUrl,
                        durationMs = durationMs,
                        currentPositionMs = 0L
                    )

                    _incomingCast.tryEmit(payload)
                    sendJsonResponse(output, 200, """{"status":"CAST_STARTED","title":"$title"}""")
                }

                path == "/api/files/upload" && method == "POST" -> {
                    if (!isRequestAuthorized(headers, clientIp)) {
                        sendJsonResponse(output, 401, """{"error":"Unauthorized. Provide valid Bearer token or x-auth-pin"}""")
                        return
                    }

                    if (contentLength > MAX_UPLOAD_SIZE) {
                        sendJsonResponse(output, 413, """{"error":"Payload Too Large. Max file size is 500MB"}""")
                        return
                    }

                    // Read sanitized file name from headers with path traversal protection
                    val rawFileName = headers["x-file-name"] ?: "received_file_${System.currentTimeMillis()}"
                    val decodedFileName = try { URLDecoder.decode(rawFileName, "UTF-8") } catch (e: Exception) { rawFileName }
                    val safeFileName = File(decodedFileName).name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val fileSize = headers["x-file-size"]?.toLongOrNull() ?: contentLength.toLong()
                    val categoryStr = headers["x-file-category"] ?: "OTHER"
                    val category = try { FileCategory.valueOf(categoryStr) } catch (e: Exception) { FileCategory.OTHER }

                    val filesDir = File(context.filesDir, "tv_shared_files").apply { mkdirs() }
                    val targetFile = File(filesDir, safeFileName).canonicalFile

                    // Strict path traversal validation
                    if (!targetFile.path.startsWith(filesDir.canonicalPath + File.separator) && targetFile.parentFile != filesDir) {
                        sendJsonResponse(output, 403, """{"error":"Path traversal forbidden"}""")
                        return
                    }

                    val initialSharedFile = SharedFile(
                        fileName = safeFileName,
                        fileSize = fileSize,
                        formattedSize = NetworkUtils.formatFileSize(fileSize),
                        category = category,
                        localUriOrPath = targetFile.absolutePath,
                        progress = 0f,
                        speedMbps = 45.0,
                        isIncoming = true,
                        status = TransferStatus.TRANSFERRING
                    )
                    _incomingFileTransfer.tryEmit(initialSharedFile)

                    val buffer = ByteArray(64 * 1024)
                    var bytesReadTotal = 0L
                    FileOutputStream(targetFile).use { fos ->
                        while (bytesReadTotal < contentLength) {
                            val maxToRead = ((contentLength - bytesReadTotal).coerceAtMost(buffer.size.toLong())).toInt()
                            val bytesRead = input.read(buffer, 0, maxToRead)
                            if (bytesRead == -1) break
                            fos.write(buffer, 0, bytesRead)
                            bytesReadTotal += bytesRead
                            val progress = if (contentLength > 0) (bytesReadTotal.toFloat() / contentLength) else 1f
                            _incomingFileTransfer.tryEmit(
                                initialSharedFile.copy(
                                    progress = progress,
                                    status = if (bytesReadTotal >= contentLength) TransferStatus.COMPLETED else TransferStatus.TRANSFERRING
                                )
                            )
                        }
                    }

                    _incomingFileTransfer.tryEmit(
                        initialSharedFile.copy(
                            progress = 1.0f,
                            status = TransferStatus.COMPLETED,
                            localUriOrPath = targetFile.absolutePath
                        )
                    )

                    sendJsonResponse(output, 200, """{"status":"FILE_RECEIVED","name":"$safeFileName","size":$bytesReadTotal}""")
                }

                path == "/api/screen/frame" && method == "POST" -> {
                    if (contentLength > 0 && contentLength < 10 * 1024 * 1024) { // Max 10MB per frame
                        val frameBytes = readExactBodyBytes(input, contentLength)
                        if (frameBytes.size == contentLength) {
                            val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                            if (bitmap != null) {
                                _incomingScreenFrame.value = bitmap
                                _screenMirrorActive.value = true
                            }
                        }
                    }
                    sendJsonResponse(output, 200, """{"status":"FRAME_RECEIVED"}""")
                }

                path == "/api/screen/start" && method == "POST" -> {
                    val bodyBytes = readExactBodyBytes(input, contentLength)
                    val body = String(bodyBytes, Charsets.UTF_8)
                    val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
                    val title = json.optString("title", "Live Device Screen")
                    _screenMirrorSourceTitle.value = title
                    _screenMirrorActive.value = true
                    sendJsonResponse(output, 200, """{"status":"MIRROR_STARTED"}""")
                }

                path == "/api/screen/stop" && method == "POST" -> {
                    _screenMirrorActive.value = false
                    _incomingScreenFrame.value = null
                    sendJsonResponse(output, 200, """{"status":"MIRROR_STOPPED"}""")
                }

                path == "/api/playback" && method == "GET" -> {
                    val state = _currentPlaybackState.value
                    val json = JSONObject().apply {
                        put("isPlaying", state.isPlaying)
                        put("positionMs", state.currentPositionMs)
                        put("durationMs", state.durationMs)
                        put("title", state.title)
                        put("subtitle", state.subtitle)
                        put("volume", state.volume)
                        put("isMuted", state.isMuted)
                        put("mediaType", state.mediaType.name)
                        put("streamUrl", state.streamUrl)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                else -> {
                    sendJsonResponse(output, 404, """{"error":"Not Found"}""")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client socket handler error: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            413 -> "Payload Too Large"
            else -> "Internal Server Error"
        }
        val bytes = json.toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    fun stop() {
        _isRunning.value = false
        serverJob?.cancel()
        beaconJob?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}
