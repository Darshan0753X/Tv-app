package com.example.network.client

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.CastPayload
import com.example.data.model.FileCategory
import com.example.data.model.PlaybackState
import com.example.data.model.RemoteCommand
import com.example.data.model.SharedFile
import com.example.data.model.TransferStatus
import com.example.data.model.TvDevice
import com.example.network.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TvRemoteClient(private val context: Context) {

    companion object {
        private const val TAG = "TvRemoteClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val OCTET_STREAM_MEDIA_TYPE = "application/octet-stream".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Cache session tokens per device ID
    private val sessionTokens = ConcurrentHashMap<String, String>()

    fun setSessionToken(deviceId: String, token: String) {
        sessionTokens[deviceId] = token
    }

    fun getSessionToken(deviceId: String): String? = sessionTokens[deviceId]

    private fun Request.Builder.withAuth(device: TvDevice): Request.Builder {
        val token = sessionTokens[device.id]
        if (!token.isNullOrBlank()) {
            header("Authorization", "Bearer $token")
        }
        if (device.pin.isNotBlank()) {
            header("x-auth-pin", device.pin)
        }
        return this
    }

    /**
     * Authenticate and pair with TV using PIN
     */
    suspend fun pairWithPin(device: TvDevice, pin: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("pin", pin)
                put("clientName", "Phone Remote")
            }
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/pair")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val resJson = JSONObject(body)
                    val token = resJson.optString("token", "")
                    if (token.isNotBlank()) {
                        sessionTokens[device.id] = token
                        return@withContext Pair(true, token)
                    }
                    return@withContext Pair(true, null)
                }
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pairing error with ${device.ipAddress}: ${e.message}")
            Pair(false, null)
        }
    }

    /**
     * Send remote button command (DPAD, Volume, Power, Media, Text)
     */
    suspend fun sendCommand(device: TvDevice, command: RemoteCommand): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("command", command.commandType.name)
                put("textValue", command.textValue)
                put("intValue", command.intValue)
                put("floatX", command.floatX)
                put("floatY", command.floatY)
                put("timestamp", command.timestamp)
            }

            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/remote/command")
                .withAuth(device)
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.d(TAG, "Command failed to ${device.ipAddress}: ${e.message}")
            false
        }
    }

    /**
     * Cast media stream, YouTube URL, local video/audio/photo or PDF to TV
     */
    suspend fun castMedia(device: TvDevice, payload: CastPayload): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", payload.id)
                put("title", payload.title)
                put("subtitle", payload.subtitle)
                put("mediaType", payload.mediaType.name)
                put("streamUrl", payload.streamUrl)
                put("durationMs", payload.durationMs)
                put("mimeType", payload.mimeType)
            }

            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/cast")
                .withAuth(device)
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cast media failed to ${device.ipAddress}: ${e.message}")
            false
        }
    }

    /**
     * Upload / Share file over high speed Wi-Fi directly to the TV box
     */
    suspend fun uploadFile(
        device: TvDevice,
        fileName: String,
        fileSize: Long,
        category: FileCategory,
        inputStreamProvider: () -> InputStream,
        onProgress: (Float, Double) -> Unit // progress 0..1, speed in Mbps
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val encodedName = URLEncoder.encode(fileName, "UTF-8")
            val startTime = System.currentTimeMillis()

            val countingRequestBody = object : RequestBody() {
                override fun contentType() = OCTET_STREAM_MEDIA_TYPE
                override fun contentLength() = fileSize

                override fun writeTo(sink: BufferedSink) {
                    val input = inputStreamProvider()
                    val buffer = ByteArray(64 * 1024)
                    var bytesWritten = 0L
                    var lastUpdate = System.currentTimeMillis()

                    input.use { stream ->
                        var read: Int
                        while (stream.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                            bytesWritten += read

                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 100 || bytesWritten == fileSize) {
                                lastUpdate = now
                                val progress = if (fileSize > 0) bytesWritten.toFloat() / fileSize else 1f
                                val elapsedSecs = (now - startTime) / 1000.0
                                val speedMbps = if (elapsedSecs > 0) ((bytesWritten * 8) / (1024.0 * 1024.0)) / elapsedSecs else 25.0
                                onProgress(progress.coerceIn(0f, 1f), speedMbps)
                            }
                        }
                    }
                }
            }

            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/files/upload")
                .withAuth(device)
                .header("x-file-name", encodedName)
                .header("x-file-size", fileSize.toString())
                .header("x-file-category", category.name)
                .post(countingRequestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "File upload failed to ${device.ipAddress}: ${e.message}")
            false
        }
    }

    /**
     * Query playback state from TV
     */
    suspend fun getPlaybackState(device: TvDevice): PlaybackState? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/playback")
                .withAuth(device)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    return@withContext PlaybackState(
                        isPlaying = json.optBoolean("isPlaying", false),
                        currentPositionMs = json.optLong("positionMs", 0L),
                        durationMs = json.optLong("durationMs", 0L),
                        title = json.optString("title", ""),
                        subtitle = json.optString("subtitle", ""),
                        volume = json.optInt("volume", 75),
                        isMuted = json.optBoolean("isMuted", false),
                        streamUrl = json.optString("streamUrl", "")
                    )
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Send real-time screen mirror frame over high speed Wi-Fi socket/HTTP
     */
    suspend fun sendScreenFrame(device: TvDevice, frameBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/screen/frame")
                .withAuth(device)
                .post(frameBytes.toRequestBody("image/jpeg".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Send screen mirror start notification to TV
     */
    suspend fun sendScreenMirrorStart(device: TvDevice, title: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
            }
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/screen/start")
                .withAuth(device)
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Send screen mirror stop notification to TV
     */
    suspend fun sendScreenMirrorStop(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/api/screen/stop")
                .withAuth(device)
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ping TV to verify reachability and get basic device info
     */
    suspend fun pingDevice(ipAddress: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://$ipAddress:$port/api/info")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
