package com.respotfck.spotifck.ui

import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Warning
import com.respotfck.spotifck.proxy.DohResolver
import com.respotfck.spotifck.proxy.DnsSettings
import com.respotfck.spotifck.webview.WebEngine
import com.respotfck.spotifck.AppIdentity
import com.respotfck.spotifck.webview.TweakPrefs
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.respotfck.spotifck.security.AppLock
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat
import com.respotfck.spotifck.R
import com.respotfck.spotifck.bridge.SpotifyBridge
import com.respotfck.spotifck.proxy.LocalProxyManager
import com.respotfck.spotifck.proxy.ProxySettings
import com.respotfck.spotifck.service.MediaNotificationService
import com.respotfck.spotifck.ui.theme.SpotifyTheme
import com.respotfck.spotifck.ui.theme.ThemePreset
import com.respotfck.spotifck.ui.theme.ThemePrefs
import com.respotfck.spotifck.update.UpdateChecker
import com.respotfck.spotifck.webview.SpotifyWebChromeClient
import com.respotfck.spotifck.webview.SpotifyWebViewClient
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private val engineState = mutableStateOf(WebEngine.SYSTEM)
    private var enginePageOk = false
    private val networkWarning = mutableStateOf<NetWarning?>(null)
    private val dismissedWarning = mutableStateOf<String?>(null)
    private var serviceStarted = false

    private val serviceEnabledState = mutableStateOf(true)
    private val materialYouState = mutableStateOf(false)
    private val amoledState = mutableStateOf(false)
    private val themePresetState = mutableStateOf(ThemePreset.DEFAULT)

    private val showSleepTimerDialog = mutableStateOf(false)
    private val sleepTimerSelectedMinutes = mutableIntStateOf(0)
    private var sleepTimer: CountDownTimer? = null
    private val sleepTimerRemainingMs = mutableLongStateOf(0L)
    private val sleepTimerActive = mutableStateOf(false)

    private val loadingProgress = mutableIntStateOf(100)
    private val updateAvailable = mutableStateOf(false)

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val btPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val sleepTimerOptions = listOf(
        0 to "Off",
        5 to "5 min",
        10 to "10 min",
        15 to "15 min",
        30 to "30 min",
        45 to "45 min",
        60 to "60 min",
        90 to "90 min",
        -1 to "End of this song"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        engineState.value = WebEngine.active(getSharedPreferences("spotilol_prefs", MODE_PRIVATE))
        AppLock.applyScreenCapturePolicy(window)

        // After an OOM kill, Android can resume directly at MainActivity
        // skipping CertificateActivity. Redirect there so the proxy starts.
        val proxyWanted = ProxySettings.enabled(getSharedPreferences("spotilol_prefs", MODE_PRIVATE))
        if (proxyWanted && !LocalProxyManager.isRunning) {
            startActivity(Intent(this, CertificateActivity::class.java))
            finish()
            return
        }

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        requestNotificationPermission()
        requestBluetoothPermission()
        val uc = UpdateChecker(this)
        updateAvailable.value = uc.hasUpdateAvailable()
        uc.autoCheck { updateAvailable.value = it }

        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("LoggedIn", false)

        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)
        themePresetState.value = ThemePrefs.preset(prefs)

        setContent {
            val serviceEnabled = serviceEnabledState.value
            val materialYou = materialYouState.value
            val amoled = amoledState.value
            val showDialog = showSleepTimerDialog.value
            val timerActive = sleepTimerActive.value
            val loadProgress = loadingProgress.intValue

            SpotifyTheme(useDynamicColor = materialYou, amoled = amoled, preset = themePresetState.value) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    AppIdentity.currentName,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                IconButton(onClick = {
                                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                }) {
                                    Box {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_settings),
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (updateAvailable.value) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Red, CircleShape)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                Switch(
                                    checked = serviceEnabled,
                                    onCheckedChange = { newValue ->
                                        serviceEnabledState.value = newValue
                                        prefs.edit()
                                            .putBoolean("ServiceOn", newValue)
                                            .apply()
                                        if (!newValue) {
                                            stopService(Intent(this@MainActivity, MediaNotificationService::class.java))
                                            serviceStarted = false
                                            destroyWebView()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (serviceEnabled && engineState.value == WebEngine.BROWSER) {
                            BrowserFallbackScreen()
                        } else if (serviceEnabled) {
                            val bridge = remember {
                                SpotifyBridge(WeakReference(this@MainActivity))
                            }

                            bridge.evalJs = { js -> webView?.post { webView?.evaluateJavascript(js, null) } }
                            bridge.onSleepTimerDone = { cancelSleepTimer() }
                            bridge.onTimerDialogRequest = {
                                showSleepTimerDialog.value = true
                                if (!timerActive) {
                                    sleepTimerSelectedMinutes.intValue = 0
                                }
                            }

                            BackHandler(enabled = webView?.canGoBack() == true) {
                                webView?.goBack()
                            }

                            AndroidView(
                                factory = { context ->
                                    val created = runCatching { WebView(context) }.getOrNull()
                                    if (created == null) {
                                        window.decorView.post { onEngineFailure("the web engine couldn't start") }
                                        android.widget.FrameLayout(context)
                                    } else created.apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )

                                        webView = this

                                        val safeMode = engineState.value == WebEngine.SAFE
                                        enginePageOk = false
                                        setLayerType(if (safeMode) View.LAYER_TYPE_NONE else View.LAYER_TYPE_HARDWARE, null)

                                        settings.apply {
                                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            setSupportZoom(true)
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            allowFileAccess = false
                                            allowContentAccess = false
                                            mediaPlaybackRequiresUserGesture = false
                                            setSupportMultipleWindows(true)
                                            javaScriptCanOpenWindowsAutomatically = true
                                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                            setGeolocationEnabled(false)
                                            @Suppress("DEPRECATION")
                                            saveFormData = false
                                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                        }

                                        setInitialScale(100)
                                        setBackgroundColor(if (amoled) 0xFF000000.toInt() else themePresetState.value.background.toInt())

                                        if (WebViewFeature.isFeatureSupported(WebViewFeature.BACK_FORWARD_CACHE)) {
                                            WebSettingsCompat.setBackForwardCacheEnabled(settings, true)
                                        }

                                        addJavascriptInterface(bridge, "AndBridge")
                                        webChromeClient = SpotifyWebChromeClient(
                                            onProgressChanged = { progress ->
                                                loadingProgress.intValue = progress
                                            }
                                        )

                                        webViewClient = SpotifyWebViewClient(
                                            onLoginRequired = {
                                                loadUrl("https://accounts.spotify.com/login")
                                            },
                                            onPageOk = { enginePageOk = true },
                                            onRendererGone = { onEngineFailure("the web engine crashed") }
                                        )

                                        if (!proxyWanted || safeMode) {
                                            ProxyController.getInstance().clearProxyOverride(
                                                Executors.newSingleThreadExecutor()
                                            ) { }
                                        } else if (LocalProxyManager.isRunning) {
                                            val executor = Executors.newSingleThreadExecutor()
                                            val proxyConfig = ProxyConfig.Builder()
                                                .addProxyRule("localhost:${LocalProxyManager.port}")
                                                .build()
                                            ProxyController.getInstance().setProxyOverride(
                                                proxyConfig,
                                                executor,
                                                { }
                                            )
                                        }

                                        val watched = this
                                        postDelayed({
                                            if (!enginePageOk && webView === watched && isOnline()) {
                                                onEngineFailure("the player didn't load within 45 seconds")
                                            }
                                        }, 45_000)

                                        if (loggedIn) {
                                            loadUrl(TweakPrefs.startPage(getSharedPreferences("spotilol_prefs", MODE_PRIVATE)).url)
                                        } else {
                                            loadUrl("https://accounts.spotify.com/login")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )

                            LaunchedEffect(webView) {
                                webView?.let { startMediaService() }
                            }

                            val progressAlpha by animateFloatAsState(
                                targetValue = if (loadProgress < 100) 1f else 0f,
                                animationSpec = tween(durationMillis = 600, delayMillis = 200),
                                label = "progressAlpha"
                            )
                            if (progressAlpha > 0.001f) {
                                LinearProgressIndicator(
                                    progress = { loadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                        .alpha(progressAlpha),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.Transparent,
                                )
                            }

                            NetworkWarningBanner(Modifier.align(Alignment.TopCenter))

                            if (showDialog) {
                                SleepTimerDialog(
                                    timerActive = timerActive,
                                    timerRemainingMs = sleepTimerRemainingMs.longValue,
                                    selectedMinutes = sleepTimerSelectedMinutes.intValue,
                                    onSelectMinute = { sleepTimerSelectedMinutes.intValue = it },
                                    onSetTimer = { minutes ->
                                        showSleepTimerDialog.value = false
                                        if (minutes != 0) {
                                            startSleepTimer(minutes)
                                        } else {
                                            cancelSleepTimer()
                                        }
                                    },
                                    onCancelTimer = {
                                        showSleepTimerDialog.value = false
                                        cancelSleepTimer()
                                    },
                                    onDismiss = {
                                        showSleepTimerDialog.value = false
                                    }
                                )
                            }
                        } else {
                            AndroidView(
                                factory = { context ->
                                    LayoutInflater.from(context)
                                        .inflate(R.layout.service_disabled, null).apply {
                                            val tvVersion = findViewById<TextView>(R.id.tvWebViewVersion)
                                            val pkg = WebViewCompat.getCurrentWebViewPackage(context)
                                            tvVersion.text = "Webview: ${pkg?.versionName ?: "N/A"}"
                                        }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                AppLockOverlay()
            }
        }
    }

    private fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes < 0) {
            sleepTimerActive.value = true
            sleepTimerRemainingMs.longValue = -1L
            webView?.evaluateJavascript(
                "window.rsStopAfterTrack&&rsStopAfterTrack(true);if(window.timerBtn) timerBtn.style.color='#2d6';",
                null
            )
            return
        }
        val totalMs = minutes * 60 * 1000L
        sleepTimerActive.value = true
        sleepTimerRemainingMs.longValue = totalMs

        webView?.evaluateJavascript(
            "if(window.timerBtn) timerBtn.style.color='#2d6';",
            null
        )

        sleepTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                sleepTimerRemainingMs.longValue = millisUntilFinished
            }

            override fun onFinish() {
                sleepTimerActive.value = false
                sleepTimerRemainingMs.longValue = 0L
                webView?.evaluateJavascript(
                    "if(window.timerBtn) timerBtn.style.color='';",
                    null
                )
                webView?.evaluateJavascript("actPlayPause(false)", null)
            }
        }.start()
    }

    private fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        sleepTimerActive.value = false
        sleepTimerRemainingMs.longValue = 0L
        webView?.evaluateJavascript(
            "if(window.timerBtn) timerBtn.style.color='';window.rsStopAfterTrack&&rsStopAfterTrack(false);",
            null
        )
    }

    @Composable
    private fun SleepTimerDialog(
        timerActive: Boolean,
        timerRemainingMs: Long,
        selectedMinutes: Int,
        onSelectMinute: (Int) -> Unit,
        onSetTimer: (Int) -> Unit,
        onCancelTimer: () -> Unit,
        onDismiss: () -> Unit
    ) {
        if (timerActive) {
            val remainingSecs = timerRemainingMs / 1000
            val mins = remainingSecs / 60
            val secs = remainingSecs % 60
            val timeStr = if (timerRemainingMs < 0) "Stops after this song" else String.format("%d:%02d min remaining", mins, secs)

            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = {
                    Text(
                        "Sleep Timer",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⏰",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Timer active",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onCancelTimer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel Timer")
                        }
                    }
                },
                dismissButton = {}
            )
        } else {
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = {
                    Text(
                        "Sleep Timer",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(Modifier.selectableGroup()) {
                        sleepTimerOptions.forEach { (minutes, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = selectedMinutes == minutes,
                                        onClick = { onSelectMinute(minutes) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMinutes == minutes,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { onSetTimer(selectedMinutes) },
                            enabled = selectedMinutes > 0
                        ) {
                            Text(
                                if (selectedMinutes > 0) "Set Timer" else "Off"
                            )
                        }
                    }
                },
                dismissButton = {}
            )
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(android.net.ConnectivityManager::class.java) ?: return true
        return cm.activeNetwork != null
    }

    /** Moves to the next engine in the fallback chain and restarts the screen with it. */
    private fun onEngineFailure(reason: String) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            val next = WebEngine.next(engineState.value)
            if (!WebEngine.autoFallback(prefs) || next == null) {
                Toast.makeText(this, "ReSpotifuck: ${reason}.", Toast.LENGTH_LONG).show()
                return@runOnUiThread
            }
            WebEngine.fallBack(prefs, next, reason)
            Toast.makeText(this, "${reason.replaceFirstChar { it.uppercase() }}. Switching to ${WebEngine.label(next)}.", Toast.LENGTH_LONG).show()
            recreate()
        }
    }

    private fun restartApp() {
        LocalProxyManager.stop()
        startActivity(
            Intent(this, CertificateActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun computeNetworkWarning(): NetWarning? {
        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        if (WebEngine.isFallback(prefs)) {
            return NetWarning(
                "Running in ${WebEngine.label(engineState.value)} because ${WebEngine.reason(prefs) ?: "the web engine failed"}.",
                "Retry"
            ) { WebEngine.reset(prefs); recreate() }
        }
        if (!ProxySettings.enabled(prefs)) {
            return NetWarning(
                "The local proxy is off. Secure DNS and proxy ad filtering aren't active, and Spotify may not load.",
                "Turn on"
            ) { prefs.edit().putBoolean(ProxySettings.ENABLED, true).apply(); restartApp() }
        }
        if (!LocalProxyManager.isRunning) {
            return NetWarning("The local proxy isn't running, so secure DNS isn't being used.", "Restart") { restartApp() }
        }
        if (!DnsSettings.enabled(prefs)) {
            return NetWarning("Secure DNS is off. Lookups use your phone's normal DNS and trackers aren't blocked.", "Turn on") {
                prefs.edit().putBoolean(DnsSettings.ENABLED, true).apply()
                DohResolver.configure(prefs)
            }
        }
        if (System.currentTimeMillis() - DohResolver.lastFallbackAt < 60_000) {
            return NetWarning("Your secure DNS provider can't be reached, so your phone's DNS is being used for now.", "Settings") {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        return null
    }

    @Composable
    private fun NetworkWarningBanner(modifier: Modifier) {
        LaunchedEffect(Unit) {
            while (true) {
                networkWarning.value = computeNetworkWarning()
                kotlinx.coroutines.delay(3000)
            }
        }
        val w = networkWarning.value ?: return
        if (dismissedWarning.value == w.text) return
        Surface(
            modifier = modifier.padding(10.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shadowElevation = 6.dp
        ) {
            Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Warning")
                Spacer(Modifier.width(10.dp))
                Text(w.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { w.action(); networkWarning.value = computeNetworkWarning() }) { Text(w.actionLabel, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { dismissedWarning.value = w.text }) { Text("Dismiss") }
            }
        }
    }

    @Composable
    private fun BrowserFallbackScreen() {
        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Browser mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                (WebEngine.reason(prefs)?.let { "ReSpotifuck switched here because $it. " } ?: "") +
                    "Spotify opens in your browser. Ad filtering and the custom player aren't available in this mode.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://open.spotify.com"))) }
            }) { Text("Open Spotify in browser") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                WebEngine.reset(prefs)
                engineState.value = WebEngine.active(prefs)
                recreate()
            }) { Text("Try the web engine again") }
        }
    }

    private fun destroyWebView() {
        webView?.let {
            it.stopLoading()
            it.removeJavascriptInterface("AndBridge")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE)) {
                try {
                    WebViewCompat.getWebViewRenderProcess(it)?.terminate()
                } catch (_: Exception) {}
            }
            // Must detach from its parent before destroy()
            // Otherwise the WebView and its renderer will be left in a bad state;
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        MediaNotificationService.webView = null
    }

    private fun startMediaService() {
        if (MediaNotificationService.instance != null) {
            MediaNotificationService.webView = webView
            return
        }
        if (serviceStarted) return
        serviceStarted = true
        MediaNotificationService.webView = webView
        val intent = Intent(this, MediaNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                btPermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        webView?.evaluateJavascript("""
            try {
                document.querySelectorAll('video').forEach(function(v) {
                    if(v.muted || v.hasAttribute('loop') || v.style.objectFit === 'cover') {
                        v.pause();
                        v.removeAttribute('src');
                        v.load();
                    }
                });
            } catch(e) {}
        """.trimIndent(), null)
    }

    override fun onResume() {
        super.onResume()
        if (WebEngine.active(getSharedPreferences("spotilol_prefs", MODE_PRIVATE)) != engineState.value) {
            recreate()
            return
        }
        AppLock.applyScreenCapturePolicy(window)
        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        serviceEnabledState.value = prefs.getBoolean("ServiceOn", true)
        materialYouState.value = prefs.getBoolean("MaterialYou", false)
        amoledState.value = prefs.getBoolean("AmoledTheme", false)
        themePresetState.value = ThemePrefs.preset(prefs)

        val customCss = prefs.getString("CustomCss", "") ?: ""
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)
        val closeNowPlay = prefs.getBoolean("CloseNowPlay", true)

        webView?.let { view ->
            val js = buildString {
                append("window.closeNpPref=$closeNowPlay;\n")
                append(SpotifyWebViewClient.buildAmoledJs(amoledEnabled))
                append("\n")
                append(SpotifyWebViewClient.buildThemeJs(prefs))
                append("\n")
                append(SpotifyWebViewClient.buildCustomCssJs(customCss))
                append("\n")
                append(TweakPrefs.flagsJs(prefs))
            }
            view.evaluateJavascript(js, null)

            view.evaluateJavascript(SpotifyWebViewClient.LOGOUT_CHECK_JS) { result ->
                if (result == "\"out\"") {
                    prefs.edit().putBoolean("LoggedIn", false).apply()
                    view.loadUrl("https://accounts.spotify.com/login")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val loggedIn = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
            .getBoolean("LoggedIn", false)
        if (!loggedIn) {
            webView?.loadUrl("https://accounts.spotify.com/login")
        }
    }

    override fun onDestroy() {
        cancelSleepTimer()
        webView?.let {
            it.stopLoading()
            it.clearHistory()
            it.clearFormData()
            it.removeJavascriptInterface("AndBridge")
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.destroy()
        }
        webView = null
        MediaNotificationService.webView = null
        serviceStarted = false
        super.onDestroy()
    }
}

private class NetWarning(val text: String, val actionLabel: String, val action: () -> Unit)
