package com.respotfck.spotifck.ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.respotfck.spotifck.security.AppLock
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respotfck.spotifck.proxy.LocalProxyManager
import com.respotfck.spotifck.ui.theme.SpotifyTheme
import com.respotfck.spotifck.ui.theme.ThemePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// Long enough for the intro animation to land; runs in parallel with proxy start-up.
private const val MIN_SPLASH_MS = 1000L
private const val PIN_NONE = 0
private const val PIN_PROMPT = 1
private const val PIN_SETUP = 2

class CertificateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppLock.applyScreenCapturePolicy(window)

        setContent {
            val disclosurePrefs = getSharedPreferences("spotifck_disclosure", MODE_PRIVATE)
            var disclosureAccepted by remember {
                mutableStateOf(disclosurePrefs.getBoolean("first_run_notice_acknowledged", false))
            }
            var certInstalled by remember { mutableStateOf(false) }
            var checkDone by remember { mutableStateOf(false) }
            var checking by remember { mutableStateOf(true) }
            val scope = rememberCoroutineScope()
            var introDone by remember { mutableStateOf(false) }
            var pinStage by remember {
                mutableStateOf(if (AppLock.shouldPromptForPin()) PIN_PROMPT else PIN_NONE)
            }
            LaunchedEffect(Unit) {
                delay(MIN_SPLASH_MS)
                introDone = true
            }

            LaunchedEffect(disclosureAccepted) {
                if (!disclosureAccepted) return@LaunchedEffect
                withContext(Dispatchers.IO) {
                    LocalProxyManager.init(this@CertificateActivity)
                    LocalProxyManager.start()
                    certInstalled = LocalProxyManager.isCAInstalled()
                    checkDone = true
                    checking = false
                }
            }

            LaunchedEffect(certInstalled, checkDone, introDone, pinStage) {
                if (checkDone && certInstalled && introDone && pinStage == PIN_NONE) {
                    val fade = ActivityOptions.makeCustomAnimation(
                        this@CertificateActivity, android.R.anim.fade_in, android.R.anim.fade_out
                    )
                    startActivity(Intent(this@CertificateActivity, MainActivity::class.java), fade.toBundle())
                    finish()
                }
            }

            val themePrefs = remember { getSharedPreferences("spotilol_prefs", MODE_PRIVATE) }
            SpotifyTheme(
                useDynamicColor = themePrefs.getBoolean("MaterialYou", false),
                amoled = themePrefs.getBoolean("AmoledTheme", false),
                preset = ThemePrefs.preset(themePrefs)
            ) {
                if (!disclosureAccepted) {
                    LaunchSplash(status = null)
                    FirstRunDisclosure(
                        onContinue = {
                            disclosurePrefs.edit()
                                .putBoolean("first_run_notice_acknowledged", true)
                                .apply()
                            disclosureAccepted = true
                        },
                        onExit = { finish() }
                    )
                } else if (pinStage == PIN_PROMPT) {
                    LaunchSplash(status = null)
                    PinPromptDialog(
                        onSetPin = { pinStage = PIN_SETUP },
                        onSkip = {
                            AppLock.setPromptState(AppLock.PROMPT_SKIPPED)
                            AppLock.promptHandledThisLaunch = true
                            pinStage = PIN_NONE
                        },
                        onNo = {
                            AppLock.setPromptState(AppLock.PROMPT_DECLINED)
                            AppLock.promptHandledThisLaunch = true
                            pinStage = PIN_NONE
                        }
                    )
                } else if (pinStage == PIN_SETUP) {
                    PinSetupFlow(
                        onDone = {
                            AppLock.promptHandledThisLaunch = true
                            pinStage = PIN_NONE
                        },
                        onCancel = { pinStage = PIN_PROMPT }
                    )
                } else when {
                    checking || certInstalled -> {
                        LaunchSplash(status = if (checking) "Starting secure proxy…" else null)
                    }

                    !certInstalled -> {
                        CACertDialog(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            onInstall = {
                                LocalProxyManager.installCACert(this@CertificateActivity)
                            },
                            onCheck = {
                                checking = true
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (!LocalProxyManager.isRunning) {
                                            LocalProxyManager.start()
                                        }
                                        certInstalled = LocalProxyManager.isCAInstalled()
                                    }
                                    checking = false
                                }
                            },
                            onExport = {
                                scope.launch {
                                    val path = withContext(Dispatchers.IO) {
                                        LocalProxyManager.exportCACert(this@CertificateActivity)
                                    }
                                    Toast.makeText(
                                        this@CertificateActivity,
                                        "Exported to: $path",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }
                }
                AppLockOverlay()
            }
        }
    }
}

@Composable
private fun FirstRunDisclosure(onContinue: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onExit,
        title = { Text("Before you continue", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ReSpotifuck is an independent community fork. It is not affiliated with or endorsed by Spotify.")
                Text("This app loads third-party music services. They may receive your account, playback, and device data under their own terms and privacy policies.")
                Text("Ad filtering and player controls modify web-player requests and behavior. They may stop working, may conflict with service terms, and do not grant rights to copy or redistribute music. Use services and content lawfully and respect copyright and DMCA requirements.")
                Text("The app uses a proxy bound to this device to modify traffic from its WebView. Installing the app’s CA certificate lets the local proxy decrypt and change HTTPS traffic routed through it. Do not install it if you do not trust this app. You can remove it later under Android Settings > Security > Encryption & credentials > User credentials.")
                Text("This proxy is not a VPN, encrypted DNS service, or anonymity tool. It does not hide your activity from Spotify or your network provider; DNS continues to use your device’s configured resolver. The app does not send proxy traffic through a remote privacy service.")
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onExit) { Text("Exit") }
        }
    )
}

@Composable
fun CACertDialog(
    modifier: Modifier = Modifier,
    onInstall: () -> Unit,
    onCheck: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Certificate Required",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "ReSpotifuck uses a local certificate to bypass Spotify's WebView restrictions. Install it once to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Step(1, "Tap \"Export .pem\" below to save the certificate to Downloads")
                Spacer(Modifier.height(12.dp))
                Step(2, "Go to Settings > Security > Encryption & credentials > Install a certificate > CA certificate")
                Spacer(Modifier.height(12.dp))
                Step(3, "Select the file ReSpotifuck_CA.pem from your Downloads folder")
                Spacer(Modifier.height(12.dp))
                Step(4, "When prompted, choose \"Install anyway\"")
                Spacer(Modifier.height(12.dp))
                Step(5, "Come back here and tap \"Check\" below")
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onExport,
                modifier = Modifier.weight(1f)
            ) {
                Text("Export .pem", color = MaterialTheme.colorScheme.primary)
            }

            TextButton(
                onClick = onCheck,
                modifier = Modifier.weight(1f)
            ) {
                Text("Check", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 18.sp
        )
    }
}
