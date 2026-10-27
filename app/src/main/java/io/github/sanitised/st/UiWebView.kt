package io.github.sanitised.st

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StWebViewScreen(
    url: String,
    onBackToDashboard: () -> Unit,
    onOpenExternalBrowser: (String) -> Unit,
    onOpenNativeChat: () -> Unit = {}
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("SillyTavern") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // File chooser launcher for uploading characters, chats, avatars
    var activeFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = activeFilePathCallback
        if (callback != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uris: Array<Uri>? = when {
                    data?.clipData != null -> {
                        val clip = data.clipData!!
                        Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                    }
                    data?.data != null -> arrayOf(data.data!!)
                    else -> null
                }
                callback.onReceiveValue(uris)
            } else {
                callback.onReceiveValue(null)
            }
            activeFilePathCallback = null
        }
    }

    // Audio recording permission for STT / microphone voice features
    var activePermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val req = activePermissionRequest
        if (req != null) {
            if (granted) {
                req.grant(req.resources)
            } else {
                req.deny()
            }
            activePermissionRequest = null
        }
    }

    // Android back button: navigate backwards inside SillyTavern first; return to Dashboard if at start
    BackHandler {
        val wv = webViewInstance
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBackToDashboard()
        }
    }

    // Root container with full insets support (status bar, nav bar, and soft keyboard ime)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .background(Color(0xFF111116))
    ) {
        // Slim dedicated Top Bar - NEVER overlaps SillyTavern web content
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back to Dashboard Button
                IconButton(
                    onClick = onBackToDashboard,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Server Dashboard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = pageTitle.takeIf { it.isNotBlank() } ?: "SillyTavern",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Switch to Fast Native Chat
                IconButton(
                    onClick = onOpenNativeChat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Switch to Native Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Force Dismiss Splash / Bypass Preloader if stuck
                IconButton(
                    onClick = {
                        webViewInstance?.evaluateJavascript(
                            """
                            (function() {
                                var splash = document.querySelector('.splash-screen') || document.getElementById('preloader');
                                if (splash && splash.parentNode) splash.parentNode.removeChild(splash);
                                var popups = document.querySelectorAll('.popup-overlay, .popup');
                                popups.forEach(function(p) { if (p.innerText && p.innerText.includes('Initializing')) p.remove(); });
                                var preloader = document.getElementById('preloader');
                                if (preloader) preloader.remove();
                            })();
                            """.trimIndent(),
                            null
                        )
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Bypass Splash Screen if Stuck",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Reload Page Button
                IconButton(
                    onClick = { webViewInstance?.reload() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Page",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Open in External Browser
                IconButton(
                    onClick = { onOpenExternalBrowser(webViewInstance?.url ?: url) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Open in External Browser",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        // Loading Bar right below the top bar
        if (isLoading && loadingProgress < 1f) {
            LinearProgressIndicator(
                progress = { loadingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Main Web View Container - takes 100% of the remaining screen height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        isFocusable = true
                        isFocusableInTouchMode = true

                        // Accept cookies including local session cookies
                        try {
                            android.webkit.CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                setAcceptThirdPartyCookies(this@apply, true)
                            }
                        } catch (_: Exception) {}

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            javaScriptCanOpenWindowsAutomatically = true
                            mediaPlaybackRequiresUserGesture = false
                            setSupportZoom(false)
                            builtInZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "$userAgentString SillyTavernAndroidApp/1.0"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val req = request ?: return false
                                // Only intercept top-level main frame navigation!
                                // Never intercept sub-resources, iframes, web workers, blobs, or API calls!
                                if (!req.isForMainFrame) {
                                    return false
                                }

                                val targetUri = req.url ?: return false
                                val scheme = targetUri.scheme?.lowercase() ?: ""
                                // Never intercept blob:, data:, about:, or javascript: schemes
                                if (scheme != "http" && scheme != "https") {
                                    return false
                                }

                                val host = targetUri.host.orEmpty()
                                // Keep local/internal URLs inside this WebView
                                return if (host == "127.0.0.1" || host == "localhost" || host.startsWith("192.168.") || host.startsWith("10.") || host.endsWith(".local")) {
                                    false
                                } else {
                                    onOpenExternalBrowser(targetUri.toString())
                                    true
                                }
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                connectionError = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: "SillyTavern"

                                // Safety Watchdog: SillyTavern's splash screen/preloader can get stuck if an extension or tokenizer worker hangs.
                                // Automatically dismisses stuck splash overlay after 6s once the main page elements exist.
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        setTimeout(function() {
                                            try {
                                                var splash = document.querySelector('.splash-screen') || document.getElementById('preloader');
                                                var topBar = document.getElementById('top-bar');
                                                if (splash && (topBar || document.getElementById('form_sheld') || (document.body && document.body.children.length > 5))) {
                                                    console.warn('[ST-Android] Auto-dismissing stuck initialization splash screen');
                                                    if (splash.parentNode) splash.parentNode.removeChild(splash);
                                                    var popups = document.querySelectorAll('.popup-overlay, .popup');
                                                    popups.forEach(function(p) {
                                                        if (p.innerText && p.innerText.includes('Initializing')) {
                                                            p.remove();
                                                        }
                                                    });
                                                    var preloader = document.getElementById('preloader');
                                                    if (preloader) preloader.remove();
                                                }
                                            } catch(e) {}
                                        }, 6000);
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    connectionError = error?.description?.toString() ?: "Connection failed"
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }

                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                val msg = consoleMessage?.message() ?: ""
                                val line = consoleMessage?.lineNumber() ?: 0
                                val src = consoleMessage?.sourceId() ?: ""
                                android.util.Log.d("STWebView", "[$src:$line] $msg")
                                return true
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }

                            override fun onShowFileChooser(
                                view: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                activeFilePathCallback?.onReceiveValue(null)
                                activeFilePathCallback = filePathCallback

                                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }

                                return try {
                                    fileChooserLauncher.launch(intent)
                                    true
                                } catch (e: Exception) {
                                    activeFilePathCallback?.onReceiveValue(null)
                                    activeFilePathCallback = null
                                    false
                                }
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (request == null) return
                                val audioResource = PermissionRequest.RESOURCE_AUDIO_CAPTURE
                                if (request.resources.contains(audioResource)) {
                                    activePermissionRequest = request
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                } else {
                                    request.grant(request.resources)
                                }
                            }
                        }

                        webViewInstance = this
                        loadUrl(url)
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                }
            )

            // Error Retry Overlay (if Node server is still booting)
            if (connectionError != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f)),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Starting SillyTavern Server...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Connecting to $url",
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        connectionError = null
                                        isLoading = true
                                        webViewInstance?.reload()
                                    }
                                ) {
                                    Text("Retry Connection")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                androidx.compose.material3.OutlinedButton(
                                    onClick = onBackToDashboard
                                ) {
                                    Text("Dashboard")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}
