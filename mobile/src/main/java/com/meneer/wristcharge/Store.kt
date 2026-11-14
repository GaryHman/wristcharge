package com.meneer.wristcharge

import android.content.Context
import android.content.SharedPreferences

/** Small SharedPreferences wrapper for settings, alert state and the last reading. */
class Store(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("wristcharge", Context.MODE_PRIVATE)

    var settings: AlertSettings
        get() = AlertSettings(
            lowEnabled = prefs.getBoolean("lowEnabled", true),
            lowThreshold = prefs.getInt("lowThreshold", 20),
            levelAEnabled = prefs.getBoolean("levelAEnabled", true),
            levelA = prefs.getInt("levelA", 80),
            levelBEnabled = prefs.getBoolean("levelBEnabled", false),
            levelB = prefs.getInt("levelB", 90),
            fullEnabled = prefs.getBoolean("fullEnabled", true),
        )
        set(s) {
            prefs.edit()
                .putBoolean("lowEnabled", s.lowEnabled).putInt("lowThreshold", s.lowThreshold)
                .putBoolean("levelAEnabled", s.levelAEnabled).putInt("levelA", s.levelA)
                .putBoolean("levelBEnabled", s.levelBEnabled).putInt("levelB", s.levelB)
                .putBoolean("fullEnabled", s.fullEnabled)
                .apply()
        }

    var persistentEnabled: Boolean
        get() = prefs.getBoolean("persistent", false)
        set(v) { prefs.edit().putBoolean("persistent", v).apply() }

    var state: AlertState
        get() = AlertState(
            lowFired = prefs.getBoolean("st_lowFired", false),
            firedTargets = prefs.getStringSet("st_fired", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet(),
            fullFired = prefs.getBoolean("st_fullFired", false),
            lastLevel = prefs.getInt("st_lastLevel", -1).takeIf { it >= 0 },
            lastCharging = if (prefs.contains("st_lastCharging")) prefs.getBoolean("st_lastCharging", false) else null,
            lastTimeMillis = prefs.getLong("st_lastTime", 0L),
        )
        set(st) {
            prefs.edit()
                .putBoolean("st_lowFired", st.lowFired)
                .putStringSet("st_fired", st.firedTargets.map { it.toString() }.toSet())
                .putBoolean("st_fullFired", st.fullFired)
                .putInt("st_lastLevel", st.lastLevel ?: -1)
                .also { e -> st.lastCharging?.let { e.putBoolean("st_lastCharging", it) } }
                .putLong("st_lastTime", st.lastTimeMillis)
                .apply()
        }

    var reading: Reading?
        get() = if (!prefs.contains("r_time")) null else Reading(
            prefs.getInt("r_level", 0),
            prefs.getBoolean("r_charging", false),
            prefs.getBoolean("r_full", false),
            prefs.getLong("r_time", 0L),
        )
        set(r) {
            if (r == null) return
            prefs.edit().putInt("r_level", r.level).putBoolean("r_charging", r.charging)
                .putBoolean("r_full", r.full).putLong("r_time", r.timeMillis).apply()
        }
}
