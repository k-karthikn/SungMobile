package com.karthi.sungmobile.feature.appearance

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.webkit.WebStorage
import android.webkit.WebSettings
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.karthi.sungmobile.core.data.spotify.SpotifySessionStore
import com.karthi.sungmobile.core.data.spotify.SpotifySessionValidation
import com.karthi.sungmobile.core.data.spotify.SpotifySessionValidator
import com.karthi.sungmobile.core.data.spotify.extractSpotifySessionCookie
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.feature.spotify.SpotifyLibraryImportSection
import com.karthi.sungmobile.feature.spotify.SpotifyLibraryImportViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun SpotifyConnectionSettingsSection(
    sessionStore: SpotifySessionStore,
    validator: SpotifySessionValidator,
    libraryImport: SpotifyLibraryImportViewModel,
) {
    val scope = rememberCoroutineScope()
    var connected by remember(sessionStore) { mutableStateOf(false) }
    var ready by remember(sessionStore) { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(sessionStore, validator) {
        if (sessionStore.hasSession()) {
            when (validator.validate()) {
                SpotifySessionValidation.VALID -> connected = true
                SpotifySessionValidation.INVALID -> connected = false
                SpotifySessionValidation.UNAVAILABLE -> {
                    connected = false
                    error = "Spotify session is saved, but could not be verified right now. Reconnect to retry."
                }
            }
        }
        ready = true
    }

    SungSettingsGroup("Spotify account") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Spotify", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(if (connected) "Connected" else if (ready) "Not connected" else "Checking…",
                style = MaterialTheme.typography.labelMedium,
                color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Import saved tracks and playlists from your Spotify account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (connected) {
                OutlinedButton(
                    onClick = { error = null; showLogin = true },
                    enabled = ready && !saving,
                    modifier = Modifier.weight(1f),
                ) { Text("Reconnect") }
            } else {
                Button(
                    onClick = { error = null; showLogin = true },
                    enabled = ready && !saving,
                    modifier = Modifier.weight(1f),
                ) { Text("Connect Spotify") }
            }
            if (connected) TextButton(onClick = {
                scope.launch {
                    sessionStore.clearSession()
                    connected = false
                    error = null
                }
            }, enabled = !saving) { Text("Disconnect") }
        }
    }

    SpotifyLibraryImportSection(libraryImport, connected)

    if (showLogin) {
        SpotifyWebLoginDialog(
            saving = saving,
            onDismiss = { if (!saving) showLogin = false },
            onCookie = { cookie ->
                if (!saving) {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            sessionStore.saveSessionCookie(cookie)
                            when (validator.validate()) {
                                SpotifySessionValidation.VALID -> {
                                    connected = true
                                    showLogin = false
                                }
                                SpotifySessionValidation.INVALID -> {
                                    connected = false
                                    error = "Spotify rejected this session. Please sign in again."
                                    showLogin = false
                                }
                                SpotifySessionValidation.UNAVAILABLE -> {
                                    connected = false
                                    error = "Session saved, but Spotify could not verify it. Try reconnecting later."
                                    showLogin = false
                                }
                            }
                        } catch (_: Exception) {
                            error = "Could not save the Spotify session. Please try again."
                        } finally {
                            saving = false
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SpotifyWebLoginDialog(saving: Boolean, onDismiss: () -> Unit, onCookie: (String) -> Unit) {
    val context = LocalContext.current
    val latestOnCookie by rememberUpdatedState(onCookie)
    var webError by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val webViewForCleanup = rememberUpdatedState(webView)
    var lastUrl by remember { mutableStateOf<String?>(null) }
    var lastCapturedCookie by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showManualCookie by remember { mutableStateOf(false) }

    fun captureCurrentSession(): Boolean {
        val cookie = captureSignedInCookie(lastUrl ?: webView?.url) ?: return false
        if (cookie != lastCapturedCookie) {
            lastCapturedCookie = cookie
            latestOnCookie(cookie)
        }
        return true
    }

    LaunchedEffect(loading) {
        if (loading) {
            delay(30_000)
            if (loading) {
                loading = false
                webError = "Embedded Spotify sign-in is not responding. Reload, or use session-cookie entry."
            }
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Connect Spotify", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Sign in on Spotify. SungMobile saves the Spotify session cookie on this device.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            var createdWebView: WebView? = null
                            try {
                                WebView(viewContext).also {
                                    createdWebView = it
                                    webView = it
                                    Log.d(SPOTIFY_WEB_LOGIN_TAG, "WebView created")
                                }.apply {
                                    // LastWave sizes its WebView explicitly; some providers otherwise
                                    // create a zero-sized native child inside the Compose host.
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    // Keep WebView's default user agent, as Spotify checks its
                                    // client hints against the browser identity.
                                    settings.setSupportMultipleWindows(false)
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            super.onProgressChanged(view, newProgress)
                                            loading = newProgress < 100
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: android.graphics.Bitmap?,
                                        ) {
                                            super.onPageStarted(view, url, favicon)
                                            webError = null
                                            lastUrl = url
                                            Log.d(SPOTIFY_WEB_LOGIN_TAG, "Page started: ${safeSpotifyPage(url)}")
                                        }

                                        override fun doUpdateVisitedHistory(
                                            view: WebView?,
                                            url: String?,
                                            isReload: Boolean,
                                        ) {
                                            super.doUpdateVisitedHistory(view, url, isReload)
                                            if (!url.isNullOrBlank()) lastUrl = url
                                            captureCurrentSession()
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            val mainUrl = view?.url?.takeIf(String::isNotBlank) ?: url
                                            lastUrl = mainUrl
                                            CookieManager.getInstance().flush()
                                            Log.d(SPOTIFY_WEB_LOGIN_TAG, "Page finished: ${safeSpotifyPage(mainUrl)}")
                                            captureCurrentSession()
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?,
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            if (request?.isForMainFrame == true) {
                                                loading = false
                                                webError = "Spotify sign-in page could not load. Check your connection."
                                                Log.w(
                                                    SPOTIFY_WEB_LOGIN_TAG,
                                                    "Main-frame load failed (${error?.errorCode}) for ${request.url.host}: ${error?.description}",
                                                )
                                            }
                                        }

                                        override fun onReceivedHttpError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            errorResponse: android.webkit.WebResourceResponse?,
                                        ) {
                                            super.onReceivedHttpError(view, request, errorResponse)
                                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                                loading = false
                                                webError = "Spotify returned an error while opening sign-in. You can reload and try again."
                                                Log.w(
                                                    SPOTIFY_WEB_LOGIN_TAG,
                                                    "Main-frame HTTP ${errorResponse?.statusCode} for ${request.url.host}",
                                                )
                                            }
                                        }
                                    }
                                    // Start with a clean jar, then navigate immediately, matching
                                    // LastWave's login lifecycle.
                                    runCatching {
                                        val cookieManager = CookieManager.getInstance()
                                        cookieManager.removeAllCookies(null)
                                        cookieManager.flush()
                                        WebStorage.getInstance().deleteAllData()
                                    }
                                    loadUrl(SPOTIFY_LOGIN_URL)
                                }
                            } catch (failure: Exception) {
                                runCatching { createdWebView?.destroy() }
                                webView = null
                                loading = false
                                webError = "Android System WebView is unavailable. Update or enable WebView to connect Spotify."
                                Log.e(SPOTIFY_WEB_LOGIN_TAG, "Could not create Spotify WebView", failure)
                                FrameLayout(viewContext)
                            } catch (failure: LinkageError) {
                                runCatching { createdWebView?.destroy() }
                                webView = null
                                loading = false
                                webError = "This device's WebView is incompatible with Spotify sign-in."
                                Log.e(SPOTIFY_WEB_LOGIN_TAG, "Spotify WebView provider is incompatible", failure)
                                FrameLayout(viewContext)
                            }
                        },
                    )
                    if (loading) {
                        LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth())
                    }
                    webError?.let { message ->
                        Surface(Modifier.align(Alignment.Center).padding(18.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.errorContainer) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                                OutlinedButton(onClick = {
                                    webError = null
                                    loading = true
                                    webView?.reload()
                                }) { Text("Reload sign-in") }
                                TextButton(onClick = { showManualCookie = true }) {
                                    Text("Enter session cookie")
                                }
                            }
                        }
                    }
                    if (webView == null && loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SPOTIFY_LOGIN_URL)))
                        }.onFailure {
                            webError = "No browser app is available to open Spotify sign-in."
                        }
                    }, enabled = !saving) {
                        Text("Open browser")
                    }
                    TextButton(onClick = {
                        if (!captureCurrentSession()) {
                            webError = "No signed-in Spotify session detected yet. Finish sign-in, then check again."
                        }
                    }, enabled = !saving) {
                        Text("Check Login")
                    }
                    TextButton(onClick = { showManualCookie = true }, enabled = !saving) {
                        Text("Use cookie")
                    }
                    TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
                }
            }
        }
    }
    if (showManualCookie) {
        SpotifyManualCookieDialog(
            onDismiss = { showManualCookie = false },
            onSubmit = { cookie -> showManualCookie = false; latestOnCookie(cookie) },
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            webViewForCleanup.value?.let { view ->
                view.stopLoading()
                view.destroy()
            }
        }
    }

}

private const val SPOTIFY_LOGIN_URL = "https://accounts.spotify.com/en/login"

private fun isSpotifyPostAuthUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { android.net.Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme != "https" || uri.host != "accounts.spotify.com") return false
    val anonymousRoutes = setOf("login", "signup", "sign-up", "reset-password", "password-reset")
    return uri.pathSegments.none { it in anonymousRoutes }
}

private const val SPOTIFY_WEB_LOGIN_TAG = "SpotifyWebLogin"

private fun safeSpotifyPage(url: String?): String {
    val uri = url?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() } ?: return "unknown"
    return "${uri.host ?: "unknown"}${uri.encodedPath.orEmpty()}"
}

private fun captureSignedInCookie(url: String?): String? {
    if (!isSpotifyPostAuthUrl(url)) return null
    val cookieManager = CookieManager.getInstance()
    val cookie = listOfNotNull(
        url?.let(cookieManager::getCookie),
        cookieManager.getCookie("https://accounts.spotify.com"),
        cookieManager.getCookie("https://open.spotify.com"),
    ).firstNotNullOfOrNull(::extractSpotifySessionCookie)
    if (cookie != null) {
        Log.i(SPOTIFY_WEB_LOGIN_TAG, "Signed-in account page reached; session cookie available (${cookie.length} chars)")
    } else {
        Log.d(SPOTIFY_WEB_LOGIN_TAG, "Signed-in account page reached, but no session cookie was found")
    }
    return cookie
}
