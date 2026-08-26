package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.data.model.MirrorOrientation
import com.example.data.model.MirrorQuality
import com.example.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * ScreenCastEngine handles VirtualDisplay creation via MediaProjection,
 * ImageReader frame acquisition, bitmap rendering, JPEG compression,
 * and hosts a local high-performance MJPEG streaming server for Android TV receivers.
 */
class ScreenCastEngine(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private var quality: MirrorQuality = MirrorQuality.ULTRA_HD,
    private var orientation: MirrorOrientation = MirrorOrientation.PORTRAIT,
    private val onFrameCaptured: (Bitmap, ByteArray, Long) -> Unit
) {

    companion object {
        private const val TAG = "ScreenCastEngine"
        const val MJPEG_STREAM_PORT = 8088
        private const val VIRTUAL_DISPLAY_NAME = "ScreenCastVirtualDisplay"
    }

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var mjpegServerSocket: ServerSocket? = null
    private var mjpegServerJob: Job? = null
    private val isStreaming = AtomicBoolean(false)
    private val frameCounter = AtomicLong(0L)

    private val _latestFrameBytes = MutableStateFlow<ByteArray?>(null)
    val latestFrameBytes: StateFlow<ByteArray?> = _latestFrameBytes.asStateFlow()

    private val _latestBitmap = MutableStateFlow<Bitmap?>(null)
    val latestBitmap: StateFlow<Bitmap?> = _latestBitmap.asStateFlow()

    private var lastFrameTime = 0L
    private val minFrameIntervalMs: Long
        get() = (1000L / quality.fps).coerceAtLeast(16L)

    fun start(scope: CoroutineScope) {
        if (isStreaming.getAndSet(true)) return

        handlerThread = HandlerThread("ScreenCastCaptureThread").apply { start() }
        handler = Handler(handlerThread!!.looper)

        setupVirtualDisplay()
        startMjpegServer(scope)

        Log.i(TAG, "ScreenCastEngine started with quality: ${quality.label}, orientation: $orientation")
    }

    private fun setupVirtualDisplay() {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            val screenWidth = if (orientation == MirrorOrientation.PORTRAIT) {
                quality.width.coerceAtMost(metrics.widthPixels)
            } else {
                quality.height.coerceAtMost(metrics.heightPixels)
            }

            val screenHeight = if (orientation == MirrorOrientation.PORTRAIT) {
                quality.height.coerceAtMost(metrics.heightPixels)
            } else {
                quality.width.coerceAtMost(metrics.widthPixels)
            }

            val densityDpi = metrics.densityDpi

            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    processLatestImage(reader)
                }, handler)
            }

            virtualDisplay = mediaProjection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth,
                screenHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                handler
            )

            Log.i(TAG, "VirtualDisplay created: ${screenWidth}x${screenHeight} @ ${densityDpi}dpi")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create VirtualDisplay: ${e.message}", e)
        }
    }

    private fun processLatestImage(reader: ImageReader) {
        if (!isStreaming.get()) return

        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: return

            val now = System.currentTimeMillis()
            if (now - lastFrameTime < minFrameIntervalMs) {
                image.close()
                return
            }
            lastFrameTime = now

            val planes = image.planes
            if (planes.isEmpty()) {
                image.close()
                return
            }

            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val width = image.width
            val height = image.height

            val rowPadding = rowStride - pixelStride * width
            val bitmapWidth = width + rowPadding / pixelStride

            val bitmap = Bitmap.createBitmap(
                bitmapWidth,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop excess padding if needed
            val finalBitmap = if (bitmapWidth != width) {
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                bitmap.recycle()
                cropped
            } else {
                bitmap
            }

            // Compress to JPEG for high-speed Wi-Fi transmission
            val baos = ByteArrayOutputStream()
            val jpegQuality = when (quality) {
                MirrorQuality.CINEMA_4K -> 92
                MirrorQuality.ULTRA_HD -> 85
                MirrorQuality.BALANCED -> 75
                MirrorQuality.LOW_LATENCY -> 65
            }
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
            val jpegBytes = baos.toByteArray()

            val count = frameCounter.incrementAndGet()
            _latestFrameBytes.value = jpegBytes
            _latestBitmap.value = finalBitmap

            onFrameCaptured(finalBitmap, jpegBytes, count)
        } catch (e: Exception) {
            Log.d(TAG, "Frame processing tick: ${e.message}")
        } finally {
            image?.close()
        }
    }

    /**
     * Start high-performance embedded MJPEG video streaming server on port 8088
     * Any Android TV, browser, or media player can connect to http://<IP>:8088/screen.mjpg
     */
    private fun startMjpegServer(scope: CoroutineScope) {
        mjpegServerJob = scope.launch(Dispatchers.IO) {
            try {
                mjpegServerSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(MJPEG_STREAM_PORT))
                }
                Log.i(TAG, "MJPEG Screen Streaming Server listening on port $MJPEG_STREAM_PORT")

                while (isActive && isStreaming.get()) {
                    try {
                        val clientSocket = mjpegServerSocket?.accept() ?: break
                        launch(Dispatchers.IO) {
                            handleMjpegClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MJPEG Server error: ${e.message}")
            }
        }
    }

    private suspend fun handleMjpegClient(socket: Socket) {
        val boundary = "ScreenCastBoundaryFrame"
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = 10000
            val output = socket.getOutputStream()

            // MJPEG Multipart HTTP header
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: multipart/x-mixed-replace; boundary=--$boundary\r\n" +
                    "Cache-Control: no-cache, private\r\n" +
                    "Pragma: no-cache\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            output.write(header.toByteArray(Charsets.UTF_8))
            output.flush()

            while (isStreaming.get() && !socket.isClosed) {
                val frameBytes = _latestFrameBytes.value
                if (frameBytes != null && frameBytes.isNotEmpty()) {
                    val frameHeader = "--$boundary\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: ${frameBytes.size}\r\n\r\n"
                    output.write(frameHeader.toByteArray(Charsets.UTF_8))
                    output.write(frameBytes)
                    output.write("\r\n".toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                kotlinx.coroutines.delay(minFrameIntervalMs)
            }
        } catch (e: Exception) {
            Log.d(TAG, "MJPEG client disconnected: ${e.message}")
        } finally {
            try { socket.close() } catch (e: Exception) { /* ignore */ }
        }
    }

    fun updateQuality(newQuality: MirrorQuality) {
        this.quality = newQuality
    }

    fun updateOrientation(newOrientation: MirrorOrientation) {
        this.orientation = newOrientation
        try {
            virtualDisplay?.release()
            imageReader?.close()
            setupVirtualDisplay()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating orientation: ${e.message}")
        }
    }

    fun stop() {
        isStreaming.set(false)
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mjpegServerSocket?.close()
            mjpegServerSocket = null
            mjpegServerJob?.cancel()
            handlerThread?.quitSafely()
            handlerThread = null
            mediaProjection.stop()
            Log.i(TAG, "ScreenCastEngine stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ScreenCastEngine: ${e.message}")
        }
    }
}
