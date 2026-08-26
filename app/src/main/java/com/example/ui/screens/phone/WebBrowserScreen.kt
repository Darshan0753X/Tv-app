package com.example.ui.screens.phone

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BrowserBookmark
import com.example.data.model.CastPayload
import com.example.data.model.MediaType
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebBrowserScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.browserUrl.collectAsStateWithLifecycle()
    val pageTitle by viewModel.browserTitle.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarksList.collectAsStateWithLifecycle()

    var inputUrlText by remember { mutableStateOf(currentUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .padding(bottom = 80.dp)
    ) {
        // Address & Navigation Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, TvBorderDark, RoundedCornerShape(18.dp)),
            color = TvSurfaceDark
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                        modifier = Modifier.size(36.dp).testTag("browser_back")
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFFCBD5E1))
                    }

                    IconButton(
                        onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() },
                        modifier = Modifier.size(36.dp).testTag("browser_forward")
                    ) {
                        Icon(Icons.Default.ArrowForward, "Forward", tint = Color(0xFFCBD5E1))
                    }

                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(36.dp).testTag("browser_reload")
                    ) {
                        Icon(Icons.Default.Refresh, "Reload", tint = Color(0xFFCBD5E1))
                    }

                    // Push / Cast to TV button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(TvCrimsonPrimary)
                            .clickable {
                                viewModel.pushWebUrlToTv(inputUrlText, pageTitle)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("btn_cast_web_page"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "Cast to TV",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cast to TV",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // URL Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputUrlText,
                        onValueChange = { inputUrlText = it },
                        placeholder = { Text("Search or type URL...", color = Color(0xFF64748B)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("browser_url_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Language, "Web", tint = TvAccentCyan, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (inputUrlText.isNotBlank()) {
                                IconButton(onClick = { inputUrlText = "" }) {
                                    Icon(Icons.Default.Close, "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TvAccentCyan,
                            unfocusedBorderColor = TvBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                keyboardController?.hide()
                                var target = inputUrlText.trim()
                                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                    target = if (target.contains(".") && !target.contains(" ")) {
                                        "https://$target"
                                    } else {
                                        "https://www.google.com/search?q=${target.replace(" ", "+")}"
                                    }
                                }
                                inputUrlText = target
                                viewModel.setBrowserUrl(target)
                                webViewRef?.loadUrl(target)
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            var target = inputUrlText.trim()
                            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                target = if (target.contains(".") && !target.contains(" ")) {
                                    "https://$target"
                                } else {
                                    "https://www.google.com/search?q=${target.replace(" ", "+")}"
                                }
                            }
                            inputUrlText = target
                            viewModel.setBrowserUrl(target)
                            webViewRef?.loadUrl(target)
                        },
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(TvSurfaceVariant)
                            .testTag("btn_browser_go")
                    ) {
                        Icon(Icons.Default.Search, "Go", tint = TvAccentCyan)
                    }
                }
            }
        }

        // Bookmarks Quick Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks, key = { it.id }) { bm ->
                BookmarkChip(
                    bookmark = bm,
                    onClick = {
                        inputUrlText = bm.url
                        viewModel.setBrowserUrl(bm.url)
                        webViewRef?.loadUrl(bm.url)
                    }
                )
            }
        }

        // Progress bar when loading
        if (isLoading && pageLoadingProgress < 1f) {
            LinearProgressIndicator(
                progress = { pageLoadingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = TvCrimsonPrimary,
                trackColor = TvSurfaceVariant
            )
        }

        // In-App WebView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                if (url != null) {
                                    inputUrlText = url
                                    viewModel.setBrowserUrl(url)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                if (url != null) {
                                    inputUrlText = url
                                    viewModel.setBrowserUrl(url)
                                }
                                view?.title?.let { viewModel.setBrowserTitle(it) }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageLoadingProgress = newProgress / 100f
                                if (newProgress >= 100) isLoading = false
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    viewModel.setBrowserTitle(title)
                                }
                            }
                        }

                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    // Keep instance reference
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BookmarkChip(
    bookmark: BrowserBookmark,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, TvBorderDark, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("bookmark_${bookmark.id}"),
        color = TvSurfaceDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(TvCrimsonPrimary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bookmark.faviconLetter,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TvCrimsonPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}
