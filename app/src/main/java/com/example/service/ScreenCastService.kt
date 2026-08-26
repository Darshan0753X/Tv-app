package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.MirrorOrientation
import com.example.data.model.MirrorQuality
import com.example.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScreenCastManager exposes global reactive state and commands for Screen Casting
 * driven by MediaProjection and Foreground Service.
 */
object ScreenCastManager {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _capturedFramesCount = MutableStateFlow(0L)
    val capturedFramesCount: StateFlow<Long> = _capturedFramesCount.asStateFlow()

    private val _latestFrameBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val latestFrameBytes: SharedFlow<ByteArray> = _latestFrameBytes.asSharedFlow()

    private val _streamUrl = MutableStateFlow("")
    val streamUrl: StateFlow<String> = _streamUrl.asStateFlow()

    var activeEngine: ScreenCastEngine? = null
        internal set

    fun updateServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
        if (!running) {
            _capturedBitmap.value = null
            _streamUrl.value = ""
        }
    }

    fun updateFrame(bitmap: Bitmap, bytes: ByteArray, count: Long) {
        _capturedBitmap.value = bitmap
        _capturedFramesCount.value = count
        _latestFrameBytes.tryEmit(bytes)
    }

    fun setStreamUrl(url: String) {
        _streamUrl.value = url
    }
}

/**
 * ScreenCastService: Foreground Service required by Android 10+ / 14+ for MediaProjection capture.
 */
class ScreenCastService : Service() {

    companion object {
        const val TAG = "ScreenCastService"
        const val NOTIFICATION_ID = 4040
        const val CHANNEL_ID = "screen_cast_notification_channel"

        const val ACTION_START = "ACTION_START_SCREEN_CAST"
        const val ACTION_STOP = "ACTION_STOP_SCREEN_CAST"
        const val ACTION_CHANGE_ORIENTATION = "ACTION_CHANGE_ORIENTATION"
        const val ACTION_CHANGE_QUALITY = "ACTION_CHANGE_QUALITY"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        const val EXTRA_QUALITY_NAME = "EXTRA_QUALITY_NAME"
        const val EXTRA_ORIENTATION_NAME = "EXTRA_ORIENTATION_NAME"

        fun startService(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            quality: MirrorQuality = MirrorQuality.ULTRA_HD,
            orientation: MirrorOrientation = MirrorOrientation.PORTRAIT
        ) {
            val intent = Intent(context, ScreenCastService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_QUALITY_NAME, quality.name)
                putExtra(EXTRA_ORIENTATION_NAME, orientation.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ScreenCastService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaProjection: MediaProjection? = null
    private var engine: ScreenCastEngine? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                val qualityName = intent.getStringExtra(EXTRA_QUALITY_NAME) ?: MirrorQuality.ULTRA_HD.name
                val orientationName = intent.getStringExtra(EXTRA_ORIENTATION_NAME) ?: MirrorOrientation.PORTRAIT.name

                val quality = try { MirrorQuality.valueOf(qualityName) } catch (e: Exception) { MirrorQuality.ULTRA_HD }
                val orientation = try { MirrorOrientation.valueOf(orientationName) } catch (e: Exception) { MirrorOrientation.PORTRAIT }

                if (resultCode != -1 && resultData != null) {
                    startScreenCapture(resultCode, resultData, quality, orientation)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopScreenCapture()
                stopSelf()
            }
            ACTION_CHANGE_ORIENTATION -> {
                val orientationName = intent.getStringExtra(EXTRA_ORIENTATION_NAME) ?: MirrorOrientation.PORTRAIT.name
                val orientation = try { MirrorOrientation.valueOf(orientationName) } catch (e: Exception) { MirrorOrientation.PORTRAIT }
                engine?.updateOrientation(orientation)
            }
            ACTION_CHANGE_QUALITY -> {
                val qualityName = intent.getStringExtra(EXTRA_QUALITY_NAME) ?: MirrorQuality.ULTRA_HD.name
                val quality = try { MirrorQuality.valueOf(qualityName) } catch (e: Exception) { MirrorQuality.ULTRA_HD }
                engine?.updateQuality(quality)
            }
        }

        return START_NOT_STICKY
    }

    private fun startScreenCapture(
        resultCode: Int,
        resultData: Intent,
        quality: MirrorQuality,
        orientation: MirrorOrientation
    ) {
        val notification = createNotification("Casting screen to Android TV Box", quality.label)

        // Mandatory foreground service start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped by system")
                    stopScreenCapture()
                    stopSelf()
                }
            }, null)

            val localIp = NetworkUtils.getLocalIpAddress(this)
            val streamUrl = "http://$localIp:${ScreenCastEngine.MJPEG_STREAM_PORT}/screen.mjpg"
            ScreenCastManager.setStreamUrl(streamUrl)

            engine = ScreenCastEngine(
                context = this,
                mediaProjection = mediaProjection!!,
                quality = quality,
                orientation = orientation,
                onFrameCaptured = { bitmap, bytes, count ->
                    ScreenCastManager.updateFrame(bitmap, bytes, count)
                }
            )

            ScreenCastManager.activeEngine = engine
            engine!!.start(serviceScope)
            ScreenCastManager.updateServiceRunning(true)

            Log.i(TAG, "MediaProjection ScreenCastService active and streaming to $streamUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MediaProjection: ${e.message}", e)
            stopScreenCapture()
            stopSelf()
        }
    }

    private fun stopScreenCapture() {
        try {
            engine?.stop()
            engine = null
            ScreenCastManager.activeEngine = null
            mediaProjection?.stop()
            mediaProjection = null
            ScreenCastManager.updateServiceRunning(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture: ${e.message}")
        }
    }

    private fun createNotification(content: String, qualityLabel: String): Notification {
        val stopIntent = Intent(this, ScreenCastService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Casting to Android TV")
            .setContentText("$content ($qualityLabel)")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Cast", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Casting Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status while mirroring screen via MediaProjection"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
