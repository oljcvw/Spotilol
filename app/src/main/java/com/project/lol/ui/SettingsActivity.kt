package com.project.lol.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.webkit.WebViewCompat
import com.project.lol.proxy.LocalProxyManager

private val PastelOrange = Color(0xFFFFCC80)
private val DarkText = Color(0xFF191414)

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)

        setContent {
            MaterialTheme(colorScheme = SpotifyDarkColors) {
                SettingsScreen(
                    prefs = prefs,
                    onBack = { finish() },
                    onClearCache = { clearWebViewCache() },
                    onClearData = { clearAllData() }
                )
            }
        }
    }

    private fun clearWebViewCache() {
        val wv = WebView(applicationContext)
        wv.clearCache(true)
        wv.clearHistory()
        wv.destroy()
        Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
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
        prefs.edit().putBoolean("LoggedIn", false).commit()
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
    onBack: () -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit
) {
    var autoplayMode by remember { mutableStateOf(prefs.getString("APlayMode", "disabled") ?: "disabled") }
    var takeControl by remember { mutableStateOf(prefs.getBoolean("TakeControl", true)) }
    var andAuto by remember { mutableStateOf(prefs.getBoolean("AndAuto", true)) }
    var closeNowPlay by remember { mutableStateOf(prefs.getBoolean("CloseNowPlay", false)) }
    var guiMode by remember { mutableStateOf(prefs.getString("GuiMode", "csshack") ?: "csshack") }
    var amoledTheme by remember { mutableStateOf(prefs.getBoolean("AmoledTheme", false)) }
    var swipeStop by remember { mutableStateOf(prefs.getBoolean("SwipeStop", true)) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAutoPlayDialog by remember { mutableStateOf(false) }
    var showGuiModeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Back",
                            tint = DarkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PastelOrange,
                    titleContentColor = DarkText,
                    navigationIconContentColor = DarkText
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
        ) {
            SectionHeader("Player")

            val autoplayLabel = when (autoplayMode) {
                "disabled" -> "Disabled"
                "onetime" -> "One time at start"
                "permanent" -> "Permanent"
                else -> "One time at start"
            }
            SettingItem(
                title = "AutoPlay Mode",
                subtitle = autoplayLabel,
                onClick = { showAutoPlayDialog = true }
            )

            SettingSwitch(
                title = "Take Player Control",
                subtitle = "Auto-accept 'Take Control' prompt",
                checked = takeControl,
                onCheckedChange = {
                    takeControl = it
                    prefs.edit().putBoolean("TakeControl", it).apply()
                }
            )

            SettingSwitch(
                title = "Android Auto Controls",
                subtitle = "Media metadata for notifications",
                checked = andAuto,
                onCheckedChange = {
                    andAuto = it
                    prefs.edit().putBoolean("AndAuto", it).apply()
                }
            )

            SettingSwitch(
                title = "Always Close Now Playing",
                subtitle = "Auto-close the Now Playing panel",
                checked = closeNowPlay,
                onCheckedChange = {
                    closeNowPlay = it
                    prefs.edit().putBoolean("CloseNowPlay", it).apply()
                }
            )

            SectionHeader("Appearance")

            val guiLabel = when (guiMode) {
                "csshack" -> "Mobile CSS + JS"
                "bigwindow" -> "Wide Window"
                "none" -> "None"
                else -> "Mobile CSS + JS"
            }
            SettingItem(
                title = "GUI Hack Mode",
                subtitle = guiLabel,
                onClick = { showGuiModeDialog = true }
            )

            SettingSwitch(
                title = "AMOLED Theme",
                subtitle = "Pure black background (saves battery)",
                checked = amoledTheme,
                onCheckedChange = {
                    amoledTheme = it
                    prefs.edit().putBoolean("AmoledTheme", it).apply()
                }
            )

            SectionHeader("Security")

            val context = LocalContext.current

            SettingItem(
                title = "CA Certificate",
                subtitle = "Re-export certificate to Downloads",
                onClick = {
                    val path = LocalProxyManager.exportCACert(context)
                    Toast.makeText(context, "Exported to $path", Toast.LENGTH_LONG).show()
                }
            )

            SectionHeader("System")

            SettingSwitch(
                title = "Swipe to Stop Service",
                subtitle = "Kill service from recents",
                checked = swipeStop,
                onCheckedChange = {
                    swipeStop = it
                    prefs.edit().putBoolean("SwipeStop", it).apply()
                }
            )

            SettingItem(
                title = "Empty Cache",
                subtitle = "Useful if navigation is slow",
                onClick = { showClearCacheDialog = true }
            )

            SettingItem(
                title = "Empty Cache & Login Data",
                subtitle = "Clear everything and log out",
                onClick = { showClearDataDialog = true }
            )

            Spacer(Modifier.height(24.dp))
            val pkg = remember { WebViewCompat.getCurrentWebViewPackage(context) }
            Text(
                text = "WebView: ${pkg?.versionName ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
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

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Empty Cache") },
            text = { Text("This will clear the WebView cache. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    onClearCache()
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Empty Cache & Login Data") },
            text = { Text("All cookies will be deleted. On restart you'll need to log in again. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataDialog = false
                    onClearData()
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = PastelOrange,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1DB954),
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF444444)
            )
        )
    }
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
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PastelOrange
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
