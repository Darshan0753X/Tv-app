package com.example.ui.screens.tv

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.viewmodel.TvCompanionViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvBrowserScreen(
    viewModel: TvCompanionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.browserUrl.collectAsStateWithLifecycle()
    val pageTitle by viewModel.browserTitle.collectAsStateWithLifecycle()
    val cursorPosition by viewModel.virtualCursorPosition.collectAsStateWithLifecycle()
    val isCursorVisible by viewModel.isCursorVisible.collectAsStateWithLifecycle()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .testTag("tv_browser_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TV Top Browser Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, TvBorderDark, RoundedCornerShape(16.dp)),
                color = TvSurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }

                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Refresh, "Reload", tint = Color(0xFFCBD5E1))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(Icons.Default.Language, "Web", tint = TvAccentCyan, modifier = Modifier.size(20.dp))

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (pageTitle.isNotBlank()) pageTitle else currentUrl,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Use Phone Trackpad as Mouse",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvAccentCyan
                    )
                }
            }

            // WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    view?.title?.let { viewModel.setBrowserTitle(it) }
                                }
                            }

                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        if (webView.url != currentUrl) {
                            webView.loadUrl(currentUrl)
                        }
                        webViewRef = webView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Virtual Cursor overlay for TV
        if (isCursorVisible) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(cursorPosition.first.toInt(), cursorPosition.second.toInt()) }
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(TvCrimsonPrimary.copy(alpha = 0.9f))
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(6.dp, CircleShape)
            )
        }
    }
}
