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
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class TvReceiverHttpServer(private val context: Context) {

    companion object {
        const val SERVER_PORT = 8080
        const val UDP_BEACON_PORT = 8889
        const val TAG = "TvReceiverServer"
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

        // 2. Start UDP Beacon Broadcaster
        beaconJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val localIp = NetworkUtils.getLocalIpAddress(context)
                    val beaconMsg = "TV_HUB_BEACON|${_tvName.value}|$SERVER_PORT|Android TV Box|${_pinCode.value}"
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

    private fun handleClientConnection(socket: Socket) {
        try {
            socket.soTimeout = 15000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(input))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val pathWithQuery = parts[1]
            val path = pathWithQuery.substringBefore("?")

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            var contentLength = 0
            var contentType = ""

            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val colonIdx = line!!.indexOf(":")
                if (colonIdx > 0) {
                    val key = line!!.substring(0, colonIdx).trim().lowercase()
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    } else if (key == "content-type") {
                        contentType = value
                    }
                }
            }

            // Route handler
            when {
                path == "/api/info" && method == "GET" -> {
                    val localIp = NetworkUtils.getLocalIpAddress(context)
                    val json = JSONObject().apply {
                        put("name", _tvName.value)
                        put("ip", localIp)
                        put("port", SERVER_PORT)
                        put("model", "Android TV / Set-Top Box")
                        put("pin", _pinCode.value)
                        put("status", "ONLINE")
                        put("volume", _currentPlaybackState.value.volume)
                        put("isPlaying", _currentPlaybackState.value.isPlaying)
                        put("title", _currentPlaybackState.value.title)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                path == "/api/remote/command" && method == "POST" -> {
                    val body = readBody(reader, contentLength)
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
                    val body = readBody(reader, contentLength)
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
                    // Read file name from headers
                    val rawFileName = headers["x-file-name"] ?: "received_file_${System.currentTimeMillis()}"
                    val fileName = try { URLDecoder.decode(rawFileName, "UTF-8") } catch (e: Exception) { rawFileName }
                    val fileSize = headers["x-file-size"]?.toLongOrNull() ?: contentLength.toLong()
                    val categoryStr = headers["x-file-category"] ?: "OTHER"
                    val category = try { FileCategory.valueOf(categoryStr) } catch (e: Exception) { FileCategory.OTHER }

                    val filesDir = File(context.filesDir, "tv_shared_files").apply { mkdirs() }
                    val targetFile = File(filesDir, fileName)

                    // Write file stream
                    val initialSharedFile = SharedFile(
                        fileName = fileName,
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
                        var bytesRead = input.read(buffer)
                        // Read from raw socket input stream directly
                        while (bytesReadTotal < contentLength && bytesRead != -1) {
                            val toWrite = if (bytesReadTotal + bytesRead > contentLength) {
                                (contentLength - bytesReadTotal).toInt()
                            } else {
                                bytesRead
                            }
                            fos.write(buffer, 0, toWrite)
                            bytesReadTotal += toWrite
                            val progress = if (contentLength > 0) (bytesReadTotal.toFloat() / contentLength) else 1f
                            _incomingFileTransfer.tryEmit(
                                initialSharedFile.copy(
                                    progress = progress,
                                    status = if (bytesReadTotal >= contentLength) TransferStatus.COMPLETED else TransferStatus.TRANSFERRING
                                )
                            )
                            if (bytesReadTotal >= contentLength) break
                            bytesRead = input.read(buffer)
                        }
                    }

                    _incomingFileTransfer.tryEmit(
                        initialSharedFile.copy(
                            progress = 1.0f,
                            status = TransferStatus.COMPLETED,
                            localUriOrPath = targetFile.absolutePath
                        )
                    )

                    sendJsonResponse(output, 200, """{"status":"FILE_RECEIVED","name":"$fileName","size":$bytesReadTotal}""")
                }

                path == "/api/screen/frame" && method == "POST" -> {
                    val frameBytes = ByteArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val r = input.read(frameBytes, readTotal, contentLength - readTotal)
                        if (r == -1) break
                        readTotal += r
                    }
                    if (readTotal == contentLength) {
                        val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                        if (bitmap != null) {
                            _incomingScreenFrame.value = bitmap
                            _screenMirrorActive.value = true
                        }
                    }
                    sendJsonResponse(output, 200, """{"status":"FRAME_RECEIVED","size":$readTotal}""")
                }

                path == "/api/screen/start" && method == "POST" -> {
                    val body = readBody(reader, contentLength)
                    val json = JSONObject(body)
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

    private fun readBody(reader: BufferedReader, length: Int): String {
        if (length <= 0) return "{}"
        val buffer = CharArray(length)
        var total = 0
        while (total < length) {
            val read = reader.read(buffer, total, length - total)
            if (read == -1) break
            total += read
        }
        return String(buffer, 0, total)
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            404 -> "Not Found"
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
