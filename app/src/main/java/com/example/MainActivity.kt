package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.model.CommandType
import com.example.data.model.RemoteCommand
import com.example.ui.screens.MainControlScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TvCompanionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TvCompanionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                // Wi-Fi & Nearby device permissions
                val permissionsToRequest = remember {
                    val list = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                        list.add(Manifest.permission.READ_MEDIA_VIDEO)
                        list.add(Manifest.permission.READ_MEDIA_AUDIO)
                    }
                    list.toTypedArray()
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    // Permissions evaluated
                }

                LaunchedEffect(Unit) {
                    val needsPermission = permissionsToRequest.any {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (needsPermission) {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.userMessage.collect { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = msg,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { _ ->
                    MainControlScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Hardware D-Pad / Remote Key Event Interceptor for Android TV & Set-Top Boxes
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.DPAD_CENTER))
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.DPAD_UP))
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.DPAD_DOWN))
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.DPAD_LEFT))
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.DPAD_RIGHT))
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.VOLUME_UP))
                super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.VOLUME_DOWN))
                super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.MUTE))
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.PLAY_PAUSE))
                true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.NEXT))
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                viewModel.sendRemoteCommand(RemoteCommand(CommandType.PREVIOUS))
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
