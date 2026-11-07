package com.respotfck.spotifck.ui.theme

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

/** Built-in colour themes. Colours are ARGB; the first entry is the app default. */
enum class ThemePreset(
    val key: String,
    val label: String,
    val accent: Long,
    val background: Long,
    val surface: Long,
    val surfaceHigh: Long
) {
    ROSE("rose", "ReSpotifuck Red", 0xFFFF6F6F, 0xFF140A0C, 0xFF1D1013, 0xFF2A171B),
    CRIMSON("crimson", "Deep Crimson", 0xFFE5383B, 0xFF0E0607, 0xFF170B0D, 0xFF241215),
    MONO("mono", "Classic Mono", 0xFFE0E0E0, 0xFF121212, 0xFF181818, 0xFF242424),
    GREEN("green", "Spotify Green", 0xFF1ED760, 0xFF0B120D, 0xFF121A14, 0xFF1C261F),
    OCEAN("ocean", "Ocean Blue", 0xFF5AB0FF, 0xFF0A0F16, 0xFF101722, 0xFF19222F),
    VIOLET("violet", "Violet Night", 0xFFB794FF, 0xFF0E0A16, 0xFF151020, 0xFF20192E),
    AMBER("amber", "Sunset Amber", 0xFFFFB454, 0xFF130E08, 0xFF1C150D, 0xFF282015);

    companion object {
        val DEFAULT = ROSE
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** Theme preference keys (stored in the app's "spotilol_prefs" file). */
object ThemePrefs {
    const val PRESET = "ThemePreset"
    const val WEB_ACCENT = "ThemeWebAccent"
    const val WEB_TINT = "ThemeWebTint"

    fun preset(prefs: SharedPreferences) = ThemePreset.fromKey(prefs.getString(PRESET, null))
    fun webAccent(prefs: SharedPreferences) = prefs.getBoolean(WEB_ACCENT, true)
    fun webTint(prefs: SharedPreferences) = prefs.getBoolean(WEB_TINT, true)
}

fun ThemePreset.colorScheme(): ColorScheme {
    val accent = Color(accent)
    val bg = Color(background)
    val surface = Color(surface)
    val high = Color(surfaceHigh)
    return darkColorScheme(
        primary = accent,
        onPrimary = bg,
        primaryContainer = lerp(high, accent, 0.28f),
        onPrimaryContainer = lerp(Color.White, accent, 0.2f),
        inversePrimary = bg,
        secondary = lerp(accent, Color.White, 0.35f),
        onSecondary = bg,
        secondaryContainer = high,
        onSecondaryContainer = lerp(Color.White, accent, 0.15f),
        tertiary = lerp(accent, Color.White, 0.55f),
        onTertiary = bg,
        tertiaryContainer = high,
        onTertiaryContainer = lerp(Color.White, accent, 0.15f),
        background = bg,
        onBackground = lerp(Color.White, accent, 0.06f),
        surface = bg,
        onSurface = lerp(Color.White, accent, 0.06f),
        surfaceVariant = high,
        onSurfaceVariant = lerp(Color(0xFFB3B3B3), accent, 0.18f),
        surfaceTint = accent,
        outline = lerp(Color(0xFF767676), accent, 0.25f),
        outlineVariant = lerp(Color(0xFF3A3A3A), accent, 0.15f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = high,
        surfaceContainerHighest = lerp(high, Color.White, 0.05f),
    )
}

@Composable
fun SpotifyTheme(
    useDynamicColor: Boolean = false,
    amoled: Boolean = false,
    preset: ThemePreset = ThemePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        preset.colorScheme()
    }

    val colorScheme = if (amoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0F0F0F),
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerLowest = Color.Black,
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SairaTypography,
        content = content
    )
}
