package com.respotfck.spotifck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val guideSteps = listOf(
    "What is this?" to "ReSpotifuck opens Spotify's own web player inside an app and removes the audio ads. You still use your normal Spotify account (free works).",
    "1. Install the certificate" to "On first start the app saves ReSpotifuck_CA.pem to Downloads. Install it once: phone Settings › Security › Encryption & credentials › Install a certificate › CA certificate › pick ReSpotifuck_CA.pem. Without it Spotify won't load.",
    "2. Log in" to "Log in with your Spotify email and password (or the classic login button). You only need to do this once.",
    "3. Play music" to "Tap any song or playlist. The bar at the bottom shows the song and has play/pause, skip, shuffle, repeat, lyrics, queue and a sleep timer. Controls also appear in your notifications and on the lock screen.",
    "Proxy & secure DNS" to "These stay on. The proxy filters ads and the secure DNS (AdGuard) blocks trackers. If a red banner says they're off, tap Turn on.",
    "PIN & privacy" to "You can set a PIN so nobody else can open the app. Screenshots are blocked unless you allow them in Settings.",
    "Make it yours" to "Settings › Appearance has themes, app icons and names. Settings › Tweaks can hide podcasts, upsells and Canvas videos.",
    "If something breaks" to "Settings › System › Empty Cache fixes most loading problems. If the player keeps failing, the app switches to a backup web engine by itself. Settings › About has links to get help."
)

@Composable
fun BeginnerGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Beginner guide", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                guideSteps.forEach { (title, body) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}
