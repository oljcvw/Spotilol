package com.respotfck.spotifck

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Preset launcher icons and Spotify-related names, switched through activity-aliases in the manifest. */
object AppIdentity {
    class Icon(val id: String, val label: String, val drawable: Int, val seasonal: Boolean)
    class Name(val id: String, val label: String)

    val icons = listOf(
        Icon("default", "ReSpotifuck Red", R.drawable.respotifuck_launcher, false),
        Icon("halloween", "Halloween", R.drawable.respotifuck_icon_halloween, true),
        Icon("christmas", "Christmas", R.drawable.respotifuck_icon_christmas, true),
        Icon("valentine", "Valentine's", R.drawable.respotifuck_icon_valentine, true),
        Icon("easter", "Easter", R.drawable.respotifuck_icon_easter, true),
        Icon("summer", "Summer", R.drawable.respotifuck_icon_summer, true),
        Icon("newyear", "New Year", R.drawable.respotifuck_icon_newyear, true),
        Icon("deepfried", "Deep-fried", R.drawable.respotifuck_icon_deepfried, false),
        Icon("pixel", "Pixel", R.drawable.respotifuck_icon_pixel, false),
        Icon("upsidedown", "Upside-down", R.drawable.respotifuck_icon_upsidedown, false),
        Icon("glitch", "Glitch", R.drawable.respotifuck_icon_glitch, false),
        Icon("cursed", "Cursed", R.drawable.respotifuck_icon_cursed, false),
        Icon("cat", "Cat", R.drawable.respotifuck_icon_cat, false)
    )

    val names = listOf(
        Name("respotifuck", "ReSpotifuck"),
        Name("spotifuck", "Spotifuck"),
        Name("respotify", "ReSpotify"),
        Name("spotilol", "Spotilol"),
        Name("spotifree", "SpotiFree")
    )

    const val ICON_KEY = "AppIcon"
    const val NAME_KEY = "AppName"

    @Volatile var currentName = "ReSpotifuck"
        private set

    private fun prefs(c: Context) = c.getSharedPreferences("spotilol_prefs", Context.MODE_PRIVATE)

    fun icon(c: Context): Icon = icons.firstOrNull { it.id == prefs(c).getString(ICON_KEY, "default") } ?: icons[0]

    fun name(c: Context): Name = names.firstOrNull { it.id == prefs(c).getString(NAME_KEY, "respotifuck") } ?: names[0]

    fun load(c: Context) {
        currentName = name(c).label
    }

    fun aliasClass(icon: String, name: String) = "com.respotfck.spotifck.ui.Launcher_${icon}_$name"

    /** Enables the chosen alias before disabling the rest, so the app always keeps a launcher entry. */
    fun apply(c: Context, icon: Icon, name: Name) {
        val pm = c.packageManager
        val chosen = aliasClass(icon.id, name.id)
        pm.setComponentEnabledSetting(
            ComponentName(c.packageName, chosen),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        for (i in icons) for (n in names) {
            val cls = aliasClass(i.id, n.id)
            if (cls == chosen) continue
            pm.setComponentEnabledSetting(
                ComponentName(c.packageName, cls),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        prefs(c).edit().putString(ICON_KEY, icon.id).putString(NAME_KEY, name.id).apply()
        currentName = name.label
    }
}
