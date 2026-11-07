package com.respotfck.spotifck.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respotfck.spotifck.security.AppLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LockRed = Color(0xFFD90218)

/** Full-screen, touch-swallowing surface used for lock and PIN screens. */
@Composable
private fun SecureSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PinDots(length: Int, max: Int, shake: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.offset(x = shake.dp)
    ) {
        repeat(maxOf(length, AppLock.MIN_PIN).coerceAtMost(max)) { i ->
            Box(
                Modifier
                    .size(14.dp)
                    .background(if (i < length) LockRed else Color.Transparent, CircleShape)
                    .border(1.5.dp, if (i < length) LockRed else Color.Gray, CircleShape)
            )
        }
    }
}

@Composable
private fun PinPad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, onSubmit: () -> Unit, submitEnabled: Boolean) {
    val rows = listOf("123", "456", "789", "<0>")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = if (key == '>' && !submitEnabled) 0.03f else 0.08f), CircleShape)
                            .clickable(enabled = key != '>' || submitEnabled) {
                                when (key) {
                                    '<' -> onBackspace()
                                    '>' -> onSubmit()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            '<' -> Icon(Icons.AutoMirrored.Filled.Backspace, "Delete", tint = Color.White)
                            '>' -> Text("OK", color = if (submitEnabled) LockRed else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            else -> Text(key.toString(), color = Color.White, fontSize = 28.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Generic PIN entry. [onSubmit] returns an error message, or null on success.
 * It runs on a background dispatcher because PIN hashing is slow by design.
 */
@Composable
fun PinEntryScreen(
    title: String,
    subtitle: String,
    onSubmit: suspend (String) -> String?,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    disabledMessage: String? = null,
    initialError: String? = null
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(initialError) }
    var busy by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy || pin.length < AppLock.MIN_PIN) return
        busy = true
        val entered = pin
        scope.launch {
            val err = withContext(Dispatchers.Default) { onSubmit(entered) }
            pin = ""
            busy = false
            error = err
            if (err != null) {
                for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(45))
            }
        }
    }

    SecureSurface {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Lock, null, tint = LockRed, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.Gray, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            PinDots(pin.length, AppLock.MAX_PIN, shake.value)
            Spacer(Modifier.height(14.dp))
            Text(
                disabledMessage ?: error ?: " ",
                color = LockRed, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.height(36.dp)
            )
            Spacer(Modifier.height(10.dp))
            val padEnabled = disabledMessage == null && !busy
            PinPad(
                onDigit = { if (padEnabled && pin.length < AppLock.MAX_PIN) { pin += it; error = null } },
                onBackspace = { if (padEnabled) pin = pin.dropLast(1) },
                onSubmit = ::submit,
                submitEnabled = padEnabled && pin.length >= AppLock.MIN_PIN
            )
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onSecondary) { Text(secondaryLabel, color = Color.Gray) }
            }
        }
    }
}

/** Covers the app while [AppLock.locked] is true. Back leaves the app instead of bypassing the lock. */
@Composable
fun AppLockOverlay() {
    if (!AppLock.locked.value) return
    val activity = LocalContext.current as? Activity
    BackHandler { activity?.moveTaskToBack(true) }
    var lockoutMs by remember { mutableLongStateOf(AppLock.lockoutRemainingMs()) }
    LaunchedEffect(lockoutMs > 0) {
        while (lockoutMs > 0) {
            delay(1000)
            lockoutMs = AppLock.lockoutRemainingMs()
        }
    }
    PinEntryScreen(
        title = "ReSpotifuck is locked",
        subtitle = "Enter your PIN",
        disabledMessage = if (lockoutMs > 0) "Too many attempts. Try again in ${(lockoutMs + 999) / 1000}s" else null,
        onSubmit = { pin ->
            when (val r = AppLock.verify(pin)) {
                AppLock.Result.Ok -> { AppLock.unlock(); null }
                is AppLock.Result.Wrong -> "Wrong PIN · ${r.attemptsLeft} attempt${if (r.attemptsLeft == 1) "" else "s"} before a timeout"
                is AppLock.Result.LockedOut -> { lockoutMs = r.remainingMs; null }
            }
        }
    )
}

/** Two-step "enter, then confirm" flow. */
@Composable
fun PinSetupFlow(onDone: () -> Unit, onCancel: () -> Unit, cancelLabel: String = "Cancel") {
    var first by remember { mutableStateOf<String?>(null) }
    var mismatch by remember { mutableIntStateOf(0) }
    BackHandler { if (first != null) first = null else onCancel() }
    if (first == null) androidx.compose.runtime.key(mismatch) {
        PinEntryScreen(
            title = "Create a PIN",
            subtitle = "${AppLock.MIN_PIN}–${AppLock.MAX_PIN} digits. You'll need it to open ReSpotifuck.",
            onSubmit = { first = it; null },
            secondaryLabel = cancelLabel,
            onSecondary = onCancel,
            initialError = if (mismatch > 0) "PINs didn't match. Start again." else null
        )
    } else {
        // key() resets the entry state between the two steps.
        androidx.compose.runtime.key("confirm") {
            PinEntryScreen(
                title = "Confirm your PIN",
                subtitle = "Enter the same PIN again",
                onSubmit = { pin ->
                    if (pin == first) {
                        AppLock.setPin(pin)
                        withContext(Dispatchers.Main) { onDone() }
                        null
                    } else {
                        mismatch++
                        first = null
                        null
                    }
                },
                secondaryLabel = "Back",
                onSecondary = { first = null }
            )
        }
    }
}

/** Requires the current PIN, then calls [onVerified]. */
@Composable
fun PinVerifyFlow(title: String, onVerified: () -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    PinEntryScreen(
        title = title,
        subtitle = "Enter your current PIN",
        onSubmit = { pin ->
            when (val r = AppLock.verify(pin)) {
                AppLock.Result.Ok -> { withContext(Dispatchers.Main) { onVerified() }; null }
                is AppLock.Result.Wrong -> "Wrong PIN · ${r.attemptsLeft} left"
                is AppLock.Result.LockedOut -> "Too many attempts. Try again in ${(r.remainingMs + 999) / 1000}s"
            }
        },
        secondaryLabel = "Cancel",
        onSecondary = onCancel
    )
}

/** First-run / reinstall prompt: Set PIN, Skip (ask next launch), No (don't ask again). */
@Composable
fun PinPromptDialog(onSetPin: () -> Unit, onSkip: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Default.Lock, null, tint = LockRed) },
        title = { Text("Protect ReSpotifuck with a PIN?", fontWeight = FontWeight.Bold) },
        text = {
            Text("A PIN keeps others from opening the app and your Spotify account on this phone. You can change this later in Settings › Privacy & Lock.")
        },
        confirmButton = { TextButton(onClick = onSetPin) { Text("Set PIN", color = LockRed, fontWeight = FontWeight.Bold) } },
        dismissButton = {
            Row {
                TextButton(onClick = onNo) { Text("No") }
                TextButton(onClick = onSkip) { Text("Skip") }
            }
        }
    )
}
