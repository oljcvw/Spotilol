package com.respotfck.spotifck

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Exports and restores the app's settings. The PIN lives in a separate file and is never included. */
object SettingsBackup {
    private val excluded = setOf("LoggedIn", AppIdentity.ICON_KEY, AppIdentity.NAME_KEY)

    fun export(prefs: SharedPreferences): String {
        val out = JSONObject()
        for ((k, v) in prefs.all) {
            if (k in excluded || v == null) continue
            val e = JSONObject()
            when (v) {
                is Boolean -> e.put("t", "b").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is String -> e.put("t", "s").put("v", v)
                is Set<*> -> e.put("t", "ss").put("v", JSONArray(v.map { it.toString() }))
                else -> continue
            }
            out.put(k, e)
        }
        return JSONObject().put("app", "ReSpotifuck").put("version", 1).put("prefs", out).toString(2)
    }

    /** Returns the number of restored settings; throws if the file isn't a ReSpotifuck backup. */
    fun import(prefs: SharedPreferences, json: String): Int {
        val root = JSONObject(json)
        require(root.optString("app") == "ReSpotifuck") { "Not a ReSpotifuck backup" }
        val p = root.getJSONObject("prefs")
        val ed = prefs.edit()
        var n = 0
        for (k in p.keys()) {
            if (k in excluded) continue
            val e = p.getJSONObject(k)
            when (e.getString("t")) {
                "b" -> ed.putBoolean(k, e.getBoolean("v"))
                "i" -> ed.putInt(k, e.getInt("v"))
                "l" -> ed.putLong(k, e.getLong("v"))
                "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                "s" -> ed.putString(k, e.getString("v"))
                "ss" -> {
                    val a = e.getJSONArray("v")
                    ed.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet())
                }
                else -> continue
            }
            n++
        }
        ed.apply()
        return n
    }
}
