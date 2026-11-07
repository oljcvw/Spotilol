package com.respotfck.spotifck.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.respotfck.spotifck.security.AppLock
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.respotfck.spotifck.R
import com.respotfck.spotifck.ui.theme.SpotifyTheme
import com.respotfck.spotifck.ui.theme.ThemePreset
import com.respotfck.spotifck.ui.theme.ThemePrefs
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.webkit.WebViewCompat
import com.respotfck.spotifck.proxy.DnsBlockedException
import com.respotfck.spotifck.proxy.DnsSettings
import com.respotfck.spotifck.proxy.DohResolver
import com.respotfck.spotifck.proxy.LocalProxyManager
import com.respotfck.spotifck.proxy.ProxySettings
import com.respotfck.spotifck.update.UpdateChecker

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppLock.applyScreenCapturePolicy(window)
        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)

        setContent {
            var materialYou by remember { mutableStateOf(prefs.getBoolean("MaterialYou", false)) }
            var amoledTheme by remember { mutableStateOf(prefs.getBoolean("AmoledTheme", false)) }
            var themePreset by remember { mutableStateOf(ThemePrefs.preset(prefs)) }
            SpotifyTheme(useDynamicColor = materialYou, amoled = amoledTheme, preset = themePreset) {
                SettingsScreen(
                    prefs = prefs,
                    materialYou = materialYou,
                    onMaterialYouChange = { materialYou = it },
                    amoledThemeState = amoledTheme,
                    onAmoledThemeChange = { amoledTheme = it },
                    themePreset = themePreset,
                    onThemePresetChange = {
                        themePreset = it
                        prefs.edit().putString(ThemePrefs.PRESET, it.key).apply()
                    },
                    onBack = { finish() },
                    onClearCache = { clearWebViewCache() },
                    onClearData = { clearAllData() }
                )
                AppLockOverlay()
            }
        }
    }

    private fun clearWebViewCache() {
        val wv = WebView(applicationContext)
        wv.clearCache(true)
        wv.clearHistory()
        wv.destroy()
        Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllData() {
        val wv = WebView(applicationContext)
        wv.clearCache(true)
        wv.clearHistory()
        wv.clearFormData()
        wv.destroy()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        prefs.edit().putBoolean("LoggedIn", false).apply()
        Toast.makeText(this, "All data cleared, please login again", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    materialYou: Boolean,
    onMaterialYouChange: (Boolean) -> Unit,
    amoledThemeState: Boolean,
    onAmoledThemeChange: (Boolean) -> Unit,
    themePreset: ThemePreset = ThemePreset.DEFAULT,
    onThemePresetChange: (ThemePreset) -> Unit = {},
    onBack: () -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit
) {
    var autoplayMode by remember { mutableStateOf(prefs.getString("APlayMode", "disabled") ?: "disabled") }
    var takeControl by remember { mutableStateOf(prefs.getBoolean("TakeControl", true)) }
    var andAuto by remember { mutableStateOf(prefs.getBoolean("AndAuto", true)) }
    var closeNowPlay by remember { mutableStateOf(prefs.getBoolean("CloseNowPlay", true)) }
    var guiMode by remember { mutableStateOf(prefs.getString("GuiMode", "csshack") ?: "csshack") }
    var customCss by remember { mutableStateOf(prefs.getString("CustomCss", "") ?: "") }
    var amoledTheme by remember { mutableStateOf(amoledThemeState) }
    var swipeStop by remember { mutableStateOf(prefs.getBoolean("SwipeStop", true)) }
    var btAutoPause by remember { mutableStateOf(prefs.getBoolean("BtAutoPause", false)) }
    var btAutoResume by remember { mutableStateOf(prefs.getBoolean("BtAutoResume", false)) }

    var proxyEnabled by remember { mutableStateOf(ProxySettings.enabled(prefs)) }
    var proxyPort by remember { mutableStateOf(ProxySettings.port(prefs)) }
    var proxyReuse by remember { mutableStateOf(ProxySettings.reuseConnections(prefs)) }
    var dnsEnabled by remember { mutableStateOf(DnsSettings.enabled(prefs)) }
    var dnsFallback by remember { mutableStateOf(DnsSettings.fallback(prefs)) }
    var dnsSummary by remember { mutableStateOf(DnsSettings.summary(prefs)) }
    var proxyRestartNeeded by remember { mutableStateOf(false) }
    var showPortDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }
    var netTestResult by remember { mutableStateOf<String?>(null) }
    var netTesting by remember { mutableStateOf(false) }
    val netScope = rememberCoroutineScope()
    fun applyDns() {
        DohResolver.configure(prefs)
        dnsSummary = DnsSettings.summary(prefs)
    }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPrivacyNotice by remember { mutableStateOf(false) }
    var showAutoPlayDialog by remember { mutableStateOf(false) }
    var showGuiModeDialog by remember { mutableStateOf(false) }
    var showCustomCssDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var webAccent by remember { mutableStateOf(ThemePrefs.webAccent(prefs)) }
    var webTint by remember { mutableStateOf(ThemePrefs.webTint(prefs)) }
    var pinEnabled by remember { mutableStateOf(AppLock.isPinSet()) }
    var lockTimeout by remember { mutableStateOf(AppLock.lockTimeoutMs()) }
    var captureAllowed by remember { mutableStateOf(AppLock.isScreenCaptureAllowed()) }
    var securityFlow by remember { mutableStateOf(SecurityFlow.NONE) }
    val activity = LocalContext.current as? android.app.Activity
    fun setCapture(allowed: Boolean) {
        AppLock.setScreenCaptureAllowed(allowed)
        captureAllowed = allowed
        activity?.window?.let(AppLock::applyScreenCapturePolicy)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingSectionCard(
                title = "PLAYER",
                icon = Icons.Default.PlayCircle
            ) {
                val autoplayLabel = when (autoplayMode) {
                    "disabled" -> "Disabled"
                    "onetime" -> "One time at start"
                    "permanent" -> "Permanent"
                    else -> "One time at start"
                }
                SettingTile(
                    title = "AutoPlay Mode",
                    subtitle = autoplayLabel,
                    icon = Icons.Default.PlayCircle,
                    onClick = { showAutoPlayDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Take Player Control",
                    subtitle = "Auto-accept 'Take Control' prompt",
                    icon = Icons.Default.TouchApp,
                    checked = takeControl,
                    onCheckedChange = {
                        takeControl = it
                        prefs.edit().putBoolean("TakeControl", it).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Android Auto Controls",
                    subtitle = "Media metadata for notifications",
                    icon = Icons.Default.DirectionsCar,
                    checked = andAuto,
                    onCheckedChange = {
                        andAuto = it
                        prefs.edit().putBoolean("AndAuto", it).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Always Close Now Playing",
                    subtitle = "Auto-close the Now Playing panel",
                    icon = Icons.Default.CloseFullscreen,
                    checked = closeNowPlay,
                    onCheckedChange = {
                        closeNowPlay = it
                        prefs.edit().putBoolean("CloseNowPlay", it).apply()
                    }
                )
            }

            SettingSectionCard(
                title = "BLUETOOTH",
                icon = Icons.Default.Smartphone
            ) {
                SettingSwitchTile(
                    title = "Pause on Disconnect",
                    subtitle = "Pause when BT/headphones disconnect",
                    icon = Icons.Default.Smartphone,
                    checked = btAutoPause,
                    onCheckedChange = {
                        btAutoPause = it
                        prefs.edit().putBoolean("BtAutoPause", it).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Resume on Connect",
                    subtitle = "Resume when BT device connects",
                    icon = Icons.Default.Smartphone,
                    checked = btAutoResume,
                    onCheckedChange = {
                        btAutoResume = it
                        prefs.edit().putBoolean("BtAutoResume", it).apply()
                    }
                )
            }

            SettingSectionCard(
                title = "APPEARANCE",
                icon = Icons.Default.Palette
            ) {
                val guiLabel = when (guiMode) {
                    "csshack" -> "Mobile CSS + JS"
                    "bigwindow" -> "Wide Window"
                    "none" -> "None"
                    else -> "Mobile CSS + JS"
                }
                SettingTile(
                    title = "Theme",
                    subtitle = if (materialYou) "${themePreset.label} (Material You is on)" else themePreset.label,
                    icon = Icons.Default.ColorLens,
                    onClick = { showThemeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Theme the Spotify player",
                    subtitle = "Recolour Spotify's green buttons and highlights",
                    icon = Icons.Default.Brush,
                    checked = webAccent,
                    onCheckedChange = { enabled ->
                        webAccent = enabled
                        prefs.edit().putBoolean(ThemePrefs.WEB_ACCENT, enabled).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Tinted backgrounds",
                    subtitle = if (amoledTheme) "Off while AMOLED is on" else "Blend the theme colour into the player's backgrounds",
                    icon = Icons.Default.FormatColorFill,
                    checked = webTint,
                    onCheckedChange = { enabled ->
                        webTint = enabled
                        prefs.edit().putBoolean(ThemePrefs.WEB_TINT, enabled).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "GUI Hack Mode",
                    subtitle = guiLabel,
                    icon = Icons.Default.Palette,
                    onClick = { showGuiModeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Custom CSS",
                    subtitle = if (customCss.isBlank()) "None configured" else customCss,
                    icon = Icons.Default.Code,
                    onClick = { showCustomCssDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "Material You Theme",
                    subtitle = "Use Android system dynamic colors",
                    icon = Icons.Default.ColorLens,
                    checked = materialYou,
                    onCheckedChange = { enabled ->
                        onMaterialYouChange(enabled)
                        prefs.edit().putBoolean("MaterialYou", enabled).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingSwitchTile(
                    title = "AMOLED Theme",
                    subtitle = "Pure black background (saves battery)",
                    icon = Icons.Default.DarkMode,
                    checked = amoledTheme,
                    onCheckedChange = { enabled ->
                        amoledTheme = enabled
                        onAmoledThemeChange(enabled)
                        prefs.edit().putBoolean("AmoledTheme", enabled).apply()
                    }
                )
            }

            SettingSectionCard(
                title = "PRIVACY & LOCK",
                icon = Icons.Default.Lock
            ) {
                SettingSwitchTile(
                    title = "App PIN lock",
                    subtitle = if (pinEnabled) "PIN required to open ReSpotifuck" else "Off",
                    icon = Icons.Default.Lock,
                    checked = pinEnabled,
                    onCheckedChange = { enable ->
                        securityFlow = if (enable) SecurityFlow.SET_PIN else SecurityFlow.REMOVE_PIN
                    }
                )
                if (pinEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    SettingTile(
                        title = "Change PIN",
                        subtitle = "Requires your current PIN",
                        icon = Icons.Default.Password,
                        onClick = { securityFlow = SecurityFlow.CHANGE_VERIFY }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    SettingTile(
                        title = "Lock after leaving the app",
                        subtitle = AppLock.timeoutOptions.firstOrNull { it.first == lockTimeout }?.second
                            ?: "Immediately",
                        icon = Icons.Default.Timer,
                        onClick = {
                            val options = AppLock.timeoutOptions
                            val next = options[(options.indexOfFirst { it.first == lockTimeout } + 1) % options.size].first
                            AppLock.setLockTimeoutMs(next)
                            lockTimeout = next
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                SettingSwitchTile(
                    title = "Allow screen capture",
                    subtitle = if (captureAllowed) "Screenshots and recording allowed" else "Blocked (recommended)",
                    icon = Icons.Default.Screenshot,
                    checked = captureAllowed,
                    onCheckedChange = { allow ->
                        if (allow) securityFlow = SecurityFlow.CAPTURE_PUZZLE else setCapture(false)
                    }
                )
            }

            SettingSectionCard(
                title = "PROXY & DNS",
                icon = Icons.Default.Dns
            ) {
                val context = LocalContext.current
                SettingSwitchTile(
                    title = "Local proxy",
                    subtitle = if (proxyEnabled) {
                        "On · port " + (if (proxyPort == 0) "automatic" else proxyPort.toString()) +
                            (if (LocalProxyManager.isRunning) " (running on ${LocalProxyManager.port})" else "")
                    } else "Off · Spotify may refuse the web player and DNS settings won't apply",
                    icon = Icons.Default.SettingsEthernet,
                    checked = proxyEnabled,
                    onCheckedChange = {
                        proxyEnabled = it
                        prefs.edit().putBoolean(ProxySettings.ENABLED, it).apply()
                        proxyRestartNeeded = true
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                SettingTile(
                    title = "Proxy port",
                    subtitle = if (proxyPort == 0) "Automatic (recommended)" else "Fixed: $proxyPort",
                    icon = Icons.Default.Router,
                    onClick = { showPortDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                SettingSwitchTile(
                    title = "Reuse connections",
                    subtitle = "Keep Spotify connections open between requests (faster)",
                    icon = Icons.Default.Speed,
                    checked = proxyReuse,
                    onCheckedChange = {
                        proxyReuse = it
                        prefs.edit().putBoolean(ProxySettings.REUSE, it).apply()
                        LocalProxyManager.reuseConnections = it
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                SettingSwitchTile(
                    title = "Secure DNS (DNS-over-HTTPS)",
                    subtitle = dnsSummary,
                    icon = Icons.Default.Dns,
                    checked = dnsEnabled,
                    onCheckedChange = {
                        dnsEnabled = it
                        prefs.edit().putBoolean(DnsSettings.ENABLED, it).apply()
                        applyDns()
                    }
                )
                if (dnsEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    SettingTile(
                        title = "DNS provider",
                        subtitle = DnsSettings.endpoint(prefs),
                        icon = Icons.Default.Public,
                        onClick = { showDnsDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    SettingSwitchTile(
                        title = "Fall back to device DNS",
                        subtitle = "If the DNS provider can't be reached",
                        icon = Icons.Default.SettingsBackupRestore,
                        checked = dnsFallback,
                        onCheckedChange = {
                            dnsFallback = it
                            prefs.edit().putBoolean(DnsSettings.FALLBACK, it).apply()
                            applyDns()
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                SettingTile(
                    title = if (netTesting) "Testing…" else "Test proxy & DNS",
                    subtitle = "Resolve and connect to Spotify through the current settings",
                    icon = Icons.Default.NetworkCheck,
                    onClick = {
                        if (netTesting) return@SettingTile
                        netTesting = true
                        netScope.launch {
                            netTestResult = withContext(Dispatchers.IO) { runNetworkTest(prefs) }
                            netTesting = false
                        }
                    }
                )
                if (proxyRestartNeeded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    SettingTile(
                        title = "Restart to apply proxy changes",
                        subtitle = "Restarts ReSpotifuck with the new proxy settings",
                        icon = Icons.Default.RestartAlt,
                        onClick = { restartForProxyChange(context) }
                    )
                }
            }

            SettingSectionCard(
                title = "SECURITY & NETWORK",
                icon = Icons.Default.Shield
            ) {
                val context = LocalContext.current
                SettingTile(
                    title = "CA Certificate",
                    subtitle = "Re-export proxy certificate to Downloads",
                    icon = Icons.Default.Shield,
                    onClick = {
                        val path = LocalProxyManager.exportCACert(context)
                        Toast.makeText(context, "Exported to $path", Toast.LENGTH_LONG).show()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Privacy & Legal Notice",
                    subtitle = "Review proxy, service, and copyright information",
                    icon = Icons.Default.Info,
                    onClick = { showPrivacyNotice = true }
                )
            }

            SettingSectionCard(
                title = "SYSTEM",
                icon = Icons.Default.PowerSettingsNew
            ) {
                SettingSwitchTile(
                    title = "Swipe to Stop Service",
                    subtitle = "Kill background service from recents",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = swipeStop,
                    onCheckedChange = {
                        swipeStop = it
                        prefs.edit().putBoolean("SwipeStop", it).apply()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Empty Cache",
                    subtitle = "Useful if player navigation is slow",
                    icon = Icons.Default.CleaningServices,
                    onClick = { showClearCacheDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Empty Cache & Login Data",
                    subtitle = "Clear everything and log out",
                    icon = Icons.Default.DeleteForever,
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }

            val updateContext = LocalContext.current
            val uc = remember { UpdateChecker(updateContext) }
            var hasUpdate by remember { mutableStateOf(uc.hasUpdateAvailable()) }
            var checkingForUpdates by remember { mutableStateOf(false) }
            var checkedVersion by remember { mutableStateOf(uc.latestVersion()) }
            SettingSectionCard(
                title = "UPDATES",
                icon = Icons.Default.SystemUpdate
            ) {
                SettingTile(
                    title = if (checkingForUpdates) "Checking for Updates…" else "Check for Updates",
                    subtitle = when {
                        checkingForUpdates -> "Checking GitHub Releases and release-assets"
                        hasUpdate -> "Version ${checkedVersion ?: "new"} is available"
                        checkedVersion != null -> "Up to date · latest v$checkedVersion"
                        else -> "Check GitHub Releases and release-assets"
                    },
                    icon = Icons.Default.SystemUpdate,
                    showBadge = hasUpdate,
                    onClick = {
                        if (!checkingForUpdates) {
                            checkingForUpdates = true
                            uc.checkForUpdates { available ->
                                checkingForUpdates = false
                                if (available == null) {
                                    Toast.makeText(updateContext, "Could not check for updates", Toast.LENGTH_SHORT).show()
                                } else {
                                    hasUpdate = available
                                    checkedVersion = uc.latestVersion()
                                    Toast.makeText(
                                        updateContext,
                                        if (available) "An update is available" else "ReSpotifuck is up to date",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                )

                if (hasUpdate) {
                    SettingTile(
                        title = "Download ReSpotifuck ${uc.latestVersion() ?: "update"}",
                        subtitle = "Open the signed APK from GitHub",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            val target = uc.downloadUrl() ?: uc.releaseUrl()
                            if (target.isNullOrBlank()) {
                                Toast.makeText(updateContext, "No download link is available yet", Toast.LENGTH_SHORT).show()
                            } else {
                                runCatching {
                                    updateContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                }.onFailure {
                                    Toast.makeText(updateContext, "Could not open the download link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            val context = LocalContext.current
            val pkg = remember { WebViewCompat.getCurrentWebViewPackage(context) }
            val packageInfo = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }.getOrNull()
            }
            val appVersionName = packageInfo?.versionName ?: "1.0.0"

            SettingSectionCard(
                title = "ABOUT",
                icon = Icons.Default.Info
            ) {
                SettingTile(
                    title = "GitHub Repository",
                    subtitle = "github.com/ReSpotF-ck/-ReSpotif-ck-App",
                    painter = painterResource(id = R.drawable.ic_github),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ReSpotF-ck/-ReSpotif-ck-App"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "Developer: N3K0",
                    subtitle = "Current maintainer · MarshMeadow",
                    icon = Icons.Default.Code,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MarshMeadow"))
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "N3K0's Website",
                    subtitle = "marshmeadow.netlify.app",
                    icon = Icons.Default.Language,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://marshmeadow.netlify.app"))
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "ReSpotifuck Version",
                    subtitle = "v$appVersionName",
                    icon = Icons.Default.Smartphone,
                    onClick = null
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                SettingTile(
                    title = "WebView Engine",
                    subtitle = pkg?.versionName ?: "System WebView",
                    icon = Icons.Default.Language,
                    onClick = null
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Forked from Spotilol by lyssadev; based on reverse-engineering work by Deviato.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    when (securityFlow) {
        SecurityFlow.NONE -> Unit
        SecurityFlow.SET_PIN, SecurityFlow.CHANGE_SET -> PinSetupFlow(
            onDone = { pinEnabled = true; securityFlow = SecurityFlow.NONE },
            onCancel = { securityFlow = SecurityFlow.NONE }
        )
        SecurityFlow.CHANGE_VERIFY -> PinVerifyFlow(
            title = "Change PIN",
            onVerified = { securityFlow = SecurityFlow.CHANGE_SET },
            onCancel = { securityFlow = SecurityFlow.NONE }
        )
        SecurityFlow.REMOVE_PIN -> PinVerifyFlow(
            title = "Turn off PIN lock",
            onVerified = {
                AppLock.removePin()
                AppLock.setPromptState(AppLock.PROMPT_DECLINED)
                pinEnabled = false
                securityFlow = SecurityFlow.NONE
            },
            onCancel = { securityFlow = SecurityFlow.NONE }
        )
        SecurityFlow.CAPTURE_PUZZLE -> CapturePuzzle(
            onSolved = { setCapture(true); securityFlow = SecurityFlow.NONE },
            onCancel = { securityFlow = SecurityFlow.NONE }
        )
    }

    if (showPrivacyNotice) {
        AlertDialog(
            onDismissRequest = { showPrivacyNotice = false },
            title = { Text("Privacy & Legal Notice", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ReSpotifuck is an independent community fork and is not affiliated with or endorsed by Spotify.")
                    Text("Third-party services may receive account, playback, and device data under their own terms and privacy policies.")
                    Text("Ad filtering and player controls modify web-player requests and behavior. Features may stop working or conflict with service terms. They do not grant rights to copy or redistribute music. Follow applicable service terms, copyright laws, and DMCA requirements.")
                    Text("The app's local proxy can decrypt and modify HTTPS traffic routed through its WebView when its CA certificate is installed. Remove that certificate from Android Settings > Security > Encryption & credentials > User credentials if you stop using the app.")
                    Text("The proxy is not a VPN, encrypted DNS service, or anonymity tool. Spotify and network providers can still observe activity, and DNS for proxied Spotify traffic uses encrypted DNS-over-HTTPS (AdGuard by default) unless you turn it off in Settings › Proxy & DNS.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyNotice = false }) { Text("Close") }
            }
        )
    }

    if (showPortDialog) {
        var text by remember { mutableStateOf(if (proxyPort == 0) "" else proxyPort.toString()) }
        val parsed = text.trim().toIntOrNull()
        val valid = text.isBlank() || (parsed != null && parsed in 1024..65535)
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = { Text("Proxy port", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Leave empty to pick a free port automatically. Fixed ports must be 1024–65535; if it's in use, an automatic port is used instead.")
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() }.take(5) },
                        singleLine = true,
                        isError = !valid,
                        placeholder = { Text("Automatic") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    val port = if (text.isBlank()) 0 else parsed ?: 0
                    if (port != proxyPort) {
                        proxyPort = port
                        prefs.edit().putInt(ProxySettings.PORT, port).apply()
                        proxyRestartNeeded = true
                    }
                    showPortDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPortDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDnsDialog) {
        var selected by remember { mutableStateOf(DnsSettings.provider(prefs).key) }
        var custom by remember { mutableStateOf(DnsSettings.customUrl(prefs)) }
        val customValid = selected != "custom" || DnsSettings.isValidDohUrl(custom)
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("DNS provider", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DnsSettings.providers.forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selected = p.key }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected == p.key, onClick = { selected = p.key })
                            Column {
                                Text(p.label)
                                if (p.url.isNotEmpty()) Text(
                                    p.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (selected == "custom") {
                        OutlinedTextField(
                            value = custom,
                            onValueChange = { custom = it.trim() },
                            singleLine = true,
                            isError = !customValid,
                            label = { Text("https://…/dns-query") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = customValid, onClick = {
                    prefs.edit()
                        .putString(DnsSettings.PROVIDER, selected)
                        .putString(DnsSettings.CUSTOM_URL, custom)
                        .apply()
                    applyDns()
                    showDnsDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDnsDialog = false }) { Text("Cancel") } }
        )
    }

    netTestResult?.let { result ->
        AlertDialog(
            onDismissRequest = { netTestResult = null },
            title = { Text("Proxy & DNS test", fontWeight = FontWeight.Bold) },
            text = { Text(result) },
            confirmButton = { TextButton(onClick = { netTestResult = null }) { Text("OK") } }
        )
    }

    if (showAutoPlayDialog) {
        SingleChoiceDialog(
            title = "AutoPlay Mode",
            options = listOf(
                "disabled" to "Disabled",
                "onetime" to "One time at start",
                "permanent" to "Permanent"
            ),
            selected = autoplayMode,
            onSelect = { value ->
                autoplayMode = value
                prefs.edit().putString("APlayMode", value).apply()
            },
            onDismiss = { showAutoPlayDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemePresetDialog(
            selected = themePreset,
            onSelect = onThemePresetChange,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showGuiModeDialog) {
        SingleChoiceDialog(
            title = "GUI Hack Mode",
            options = listOf(
                "csshack" to "Mobile CSS + JS",
                "bigwindow" to "Wide Window",
                "none" to "None"
            ),
            selected = guiMode,
            onSelect = { value ->
                guiMode = value
                prefs.edit().putString("GuiMode", value).apply()
            },
            onDismiss = { showGuiModeDialog = false }
        )
    }

    if (showCustomCssDialog) {
        CustomCssDialog(
            initialCss = customCss,
            onSave = { css ->
                customCss = css
                prefs.edit().putString("CustomCss", css).apply()
                showCustomCssDialog = false
            },
            onDismiss = { showCustomCssDialog = false }
        )
    }

    if (showClearCacheDialog) {
        ConfirmationDialog(
            title = "Empty Cache",
            message = "This will clear the WebView cache. Continue?",
            confirmText = "Clear Cache",
            onConfirm = {
                showClearCacheDialog = false
                onClearCache()
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    if (showClearDataDialog) {
        ConfirmationDialog(
            title = "Empty Cache & Login Data",
            message = "All cookies and login data will be deleted. On restart you will need to log in again. Continue?",
            confirmText = "Clear All Data",
            isDestructive = true,
            onConfirm = {
                showClearDataDialog = false
                onClearData()
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
}

@Composable
fun SettingSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingTile(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    painter: Painter? = null,
    onClick: (() -> Unit)? = null,
    isDestructive: Boolean = false,
    showBadge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Red, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingSwitchTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun ThemePresetDialog(
    selected: ThemePreset,
    onSelect: (ThemePreset) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                ThemePreset.entries.forEach { preset ->
                    val isSelected = preset == selected
                    val accent = Color(preset.accent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(preset.surface))
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) accent else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(preset) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(preset.background), CircleShape)
                                .border(2.dp, accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(16.dp).background(accent, CircleShape))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            preset.label,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) accent else Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        if (preset == ThemePreset.DEFAULT) {
                            Text("Default", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) }
        }
    )
}

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (value, label) ->
                    val isSelected = selected == value
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelect(value)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun CustomCssDialog(
    initialCss: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempCss by remember { mutableStateOf(initialCss) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Custom CSS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Injected custom CSS rules into Spotify webview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = tempCss,
                    onValueChange = { tempCss = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = {
                        Text(
                            "/* Write your CSS overrides here (use !important if needed) */\naside[data-testid=now-playing-bar] { display: none !important; }",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(tempCss) }
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("preview_prefs", Context.MODE_PRIVATE) }
    SpotifyTheme {
        SettingsScreen(
            prefs = prefs,
            materialYou = false,
            onMaterialYouChange = {},
            amoledThemeState = false,
            onAmoledThemeChange = {},
            onBack = {},
            onClearCache = {},
            onClearData = {}
        )
    }
}

private enum class SecurityFlow { NONE, SET_PIN, CHANGE_VERIFY, CHANGE_SET, REMOVE_PIN, CAPTURE_PUZZLE }

private fun runNetworkTest(prefs: SharedPreferences): String {
    val lines = mutableListOf<String>()
    val host = "open.spotify.com"
    if (DnsSettings.enabled(prefs)) {
        val endpoint = DnsSettings.endpoint(prefs)
        val t0 = System.nanoTime()
        lines += try {
            val addrs = DohResolver.lookup(endpoint, host)
            "DNS ✓ $host → ${addrs.first().hostAddress} via $endpoint (${(System.nanoTime() - t0) / 1_000_000} ms)"
        } catch (e: Exception) {
            "DNS ✗ $endpoint: ${e.message}" + if (DnsSettings.fallback(prefs)) " (device DNS will be used)" else ""
        }
        lines += try {
            DohResolver.lookup(endpoint, "doubleclick.net")
            "Ad blocking: doubleclick.net was not blocked by this provider"
        } catch (e: DnsBlockedException) {
            "Ad blocking ✓ doubleclick.net is blocked"
        } catch (e: Exception) {
            "Ad blocking: doubleclick.net lookup failed (${e.message})"
        }
    } else {
        lines += "DNS: secure DNS is off, using the device resolver"
    }
    if (!ProxySettings.enabled(prefs)) {
        lines += "Proxy: off"
    } else if (!LocalProxyManager.isRunning) {
        lines += "Proxy ✗ not running — restart the app"
    } else {
        val t0 = System.nanoTime()
        val ok = LocalProxyManager.testConnection(host)
        lines += if (ok) "Proxy ✓ HTTPS to $host through 127.0.0.1:${LocalProxyManager.port} (${(System.nanoTime() - t0) / 1_000_000} ms)"
        else "Proxy ✗ could not reach $host through 127.0.0.1:${LocalProxyManager.port}"
    }
    return lines.joinToString("\n\n")
}

private fun restartForProxyChange(context: android.content.Context) {
    LocalProxyManager.stop()
    val intent = android.content.Intent(context, CertificateActivity::class.java)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
}
